# Written by Arno Bakker, Choopan RATTANAPOKA, Jie Yang
# see LICENSE.txt for license information
#
# TODO: set 'download_slice_size' to 32K, such that pieces are no longer
# downloaded in 2 chunks. This particularly avoids a bad case where you
# kick the source: you download chunk 1 of piece X
# from lagging peer and download chunk 2 of piece X from source. With the piece
# now complete you check the sig. As the first part of the piece is old, this
# fails and we kick the peer that gave us the completing chunk, which is the 
# source.
#
# Note that the BT spec says: 
# "All current implementations use 2 15 , and close connections which request 
# an amount greater than 2 17." http://www.bittorrent.org/beps/bep_0003.html
#
# So it should be 32KB already. However, the BitTorrent (3.4.1, 5.0.9), 
# BitTornado and Azureus all use 2 ** 14 = 16KB chunks.

import os
import sys
import time
import commands
import tempfile
import urllib2
import datetime
from threading import enumerate,currentThread,RLock
from traceback import print_exc

if sys.platform == "darwin":
    # on Mac, we can only load VLC/OpenSSL libraries
    # relative to the location of tribler.py
    os.chdir(os.path.abspath(os.path.dirname(sys.argv[0])))
try:
    import wxversion
    wxversion.select('2.8')
except:
    pass
import wx

from Tribler.Core.API import *
from Tribler.Core.Utilities.unicode import bin2unicode
from Tribler.Policies.RateManager import UserDefinedMaxAlwaysOtherwiseEquallyDividedRateManager

from Tribler.Video.EmbeddedPlayer import VideoFrame,MEDIASTATE_STOPPED,MEDIASTATE_PLAYING,MEDIASTATE_PAUSED
from Tribler.Video.VideoServer import VideoHTTPServer
from Tribler.Video.VideoPlayer import VideoPlayer, VideoChooser, PLAYBACKMODE_INTERNAL, PLAYBACKMODE_EXTERNAL_DEFAULT 
from Tribler.Player.systray import PlayerTaskBarIcon
from Tribler.Utilities.Instance2Instance import *
from Tribler.Utilities.MojoCommunication import *

from Tribler.Main.Utility.utility import Utility # TO REMOVE
from Tribler.Video.utils import videoextdefaults

from Tribler.mjlogger import *

DEBUG = False
ONSCREENDEBUG = True
ALLOW_MULTIPLE = False
RATELIMITADSL = False

DISKSPACE_LIMIT = 5L * 1024L * 1024L * 1024L  # 5 GB
DEFAULT_MAX_UPLOAD_SEED_WHEN_SEEDING = 100 # KB/s
I2I_LISTENPORT = 57894
MJ_LISTENPORT = 6969
PLAYER_LISTENPORT = 8620
VIDEOHTTP_LISTENPORT = 6879

MOJOMAX_UPLOAD = 150
MOJOMAX_DOWNLOAD = 300

x = MJLogger()
x.log("updatePeerlist", False)
x.log("HELPING", False)
x.log("STILLH", False)
x.log("MAXUP", 0.0)
x.log("MAXDOWN", 0.0)
x.log("ORIGUP", 0.0)
x.log("ORIGDOWN", 0.0)
x.log("HELPEDUL", 0.0)
x.log("HELPEDDL", 0.0)
x.log("HELPEDDOWN", 0.0)

##################
x.log("CURRUL", 0)
x.log("CURRDL", 0)
x.log("ULLOG", 0)
x.log("DLLOG", 0)
x.log("SFLAG", True)
x.log("RFLAG", False)

SERVER_IP = None

origDownload = None
helpedDownload = None
totalSpeedAll = {}

class PlayerFrame(VideoFrame):

    def __init__(self,parent):
        VideoFrame.__init__(self,parent,'SwarmPlayer 1.0.1',parent.iconpath,parent.logopath)
        self.parent = parent
        
        self.Bind(wx.EVT_CLOSE, self.OnCloseWindow)
    
    def OnCloseWindow(self, event = None):
        
        self.set_wxclosing() # Call VideoFrame superclass
        
        if event is not None:
            nr = event.GetEventType()
            lookup = { wx.EVT_CLOSE.evtType[0]: "EVT_CLOSE", wx.EVT_QUERY_END_SESSION.evtType[0]: "EVT_QUERY_END_SESSION", wx.EVT_END_SESSION.evtType[0]: "EVT_END_SESSION" }
            if nr in lookup: 
                nr = lookup[nr]
            print >>sys.stderr,"main: Closing due to event ",nr
            event.Skip()
        else:
            print >>sys.stderr,"main: Closing untriggered by event"
         
        # This gets called multiple times somehow
        if self.parent.videoFrame is not None:
            self.parent.remove_current_download_if_not_complete()
            self.parent.restart_other_downloads()
            
        self.parent.videoFrame = None



class PlayerApp(wx.App):
    def __init__(self, x, params, single_instance_checker, installdir):
        self.params = params
        self.single_instance_checker = single_instance_checker
        self.installdir = installdir
        self.error = None
        self.s = None
        self.tbicon = None
        
        # TODO: Arno: we can get rid of dlock now stats processing is delegated to GUI thread:
        # Verify all places where it is used e.g. in sesscb_remove_current_callback()
        # another thread than Main uses it.  
        self.dlock = RLock()  
        self.d = None # protected by dlock
        self.playermode = DLSTATUS_DOWNLOADING # protected by dlock
        self.r = None # protected by dlock
        self.count = 0
        self.said_start_playback = False
        self.shuttingdown = False
        self.getpeerlistcount = 2
        self.decodeprogress = 0

        wx.App.__init__(self, x)
        
    def OnInit(self):
        try:
            # If already running, and user starts a new instance without a URL 
            # on the cmd line
            if not ALLOW_MULTIPLE and self.single_instance_checker.IsAnotherRunning():
                print >> sys.stderr,"main: Another instance running, no URL on CMD, asking user"
                torrentfilename = self.select_torrent_from_disk()
                if torrentfilename is not None:
                    i2ic = Instance2InstanceClient(I2I_LISTENPORT,'START',torrentfilename)
                    return False
            
            # Normal startup
            self.utility = Utility(self.installdir)
            self.utility.app = self
            print >>sys.stderr,self.utility.lang.get('build')
            self.iconpath = os.path.join(self.installdir,'Tribler','Images','swarmplayer.ico')
            self.logopath = os.path.join(self.installdir,'Tribler','Images','logoSwarmPlayer.png')

            
            # Start server for instance2instance communication
            self.i2is = Instance2InstanceServer(I2I_LISTENPORT,self.i2icallback) 
            self.i2is.start()
            
            # Start server for MojoCommunication
            self.mojoServer = MojoCommunicationServer(MJ_LISTENPORT,self.mjcallback) 
            self.mojoServer.start()
            
            # How to send message with MojoCommunication (port, msg, ipaddr)
            #MojoCommunicationClient(MJ_LISTENPORT,'Your message goes here','192.168.0.12')

            self.videoplay = None 
            self.start_video_frame()
            
            # Start HTTP server for serving video to player widget
            self.videoserv = VideoHTTPServer.getInstance(VIDEOHTTP_LISTENPORT) # create
            self.videoserv.background_serve()
            self.videoserv.register(self.videoserver_error_callback,self.videoserver_set_status_callback)

            # Initialize the player manager
            self.videoplay = VideoPlayer.getInstance()
            
            # H4xor: Use external player on Ubuntu when no VLC python bindings avail 
            if Tribler.Video.EmbeddedPlayer.REALVLC:
                playbackmode = PLAYBACKMODE_INTERNAL
            else:
                playbackmode = PLAYBACKMODE_EXTERNAL_DEFAULT
            self.videoplay.register(self.utility,overrideplaybackmode=playbackmode)
            self.videoplay.set_parentwindow(self.videoFrame)
            
            if False: # just play video file
                self.videoplay.play_from_file(self.params[0])
                return True
            
            
            # Read config
            state_dir = Session.get_default_state_dir('.SwarmPlayer')
            
            # The playerconfig contains all config parameters that are not
            # saved by checkpointing the Session or its Downloads.
            self.load_playerconfig(state_dir)

            # Install systray icon
            # Note: setting this makes the program not exit when the videoFrame
            # is being closed.
            
            self.tbicon = PlayerTaskBarIcon(self,self.iconpath)

            
            
            # Start Tribler Session
            cfgfilename = Session.get_default_config_filename(state_dir)
            
            if DEBUG:
                print >>sys.stderr,"main: Session config",cfgfilename
            try:
                self.sconfig = SessionStartupConfig.load(cfgfilename)
            except:
                print_exc()
                self.sconfig = SessionStartupConfig()
                self.sconfig.set_state_dir(state_dir)
                
                self.sconfig.set_listen_port(PLAYER_LISTENPORT)    
                self.sconfig.set_overlay(False)
                self.sconfig.set_megacache(False)

            self.s = Session(self.sconfig)
            self.s.set_download_states_callback(self.sesscb_states_callback)

            self.reporter = None #Reporter( self.sconfig )

            if RATELIMITADSL:
                self.r = UserDefinedMaxAlwaysOtherwiseEquallyDividedRateManager()
                self.r.set_global_max_speed(DOWNLOAD,400)
                self.r.set_global_max_speed(UPLOAD,90)
            
            # Load torrent
            if self.params[0] != "":
                torrentfilename = self.params[0]
                
                # TEST: just play video file
                #self.videoplay.play_url(torrentfilename)
                #return True
                
            else:
                torrentfilename = self.select_torrent_from_disk()
                if torrentfilename is None:
                    print >>sys.stderr,"main: User selected no file"
                    self.OnExit()
                    return False

            # Arno: For extra robustness, ignore any errors related to restarting
            try:
                # Load all other downloads in cache, but in STOPPED state
                self.s.load_checkpoint(initialdlstatus=DLSTATUS_STOPPED)
            except:
                print_exc()

            # Start download
            if not self.start_download(torrentfilename):
                self.OnExit()
                return False
            
        except Exception,e:
            print_exc()
            self.show_error(str(e))
            self.OnExit()
            return False
        return True

    def start_video_frame(self):
        # Start video frame
        self.videoFrame = PlayerFrame(self)
        self.Bind(wx.EVT_CLOSE, self.videoFrame.OnCloseWindow)
        self.Bind(wx.EVT_QUERY_END_SESSION, self.videoFrame.OnCloseWindow)
        self.Bind(wx.EVT_END_SESSION, self.videoFrame.OnCloseWindow)
        self.videoFrame.Show(True)

        if self.videoplay is not None:
            self.videoplay.set_parentwindow(self.videoFrame)
        self.said_start_playback = False

    def select_torrent_from_disk(self):
        dlg = wx.FileDialog(None, 
                            'SwarmPlayer: Select torrent to play', 
                            '', # default dir
                            '', # default file
                            'TSTREAM and TORRENT files (*.tstream;*.torrent)|*.tstream;*.torrent', 
                            wx.OPEN|wx.FD_FILE_MUST_EXIST)
        if dlg.ShowModal() == wx.ID_OK:
            filename = dlg.GetPath()
        else:
            filename = None
        dlg.Destroy()
        return filename

    def ask_user_to_select_video(self,videofiles):
        dlg = VideoChooser(self.videoFrame,self.utility,videofiles,title='SwarmPlayer',expl='Select which file to play')
        result = dlg.ShowModal()
        if result == wx.ID_OK:
            index = dlg.getChosenIndex()
            filename = videofiles[index]
        else:
            filename = None
        dlg.Destroy()
        return filename


    def start_download(self,torrentfilename, newTdef = None):
        
        if newTdef is None:
        	tdef = TorrentDef.load(torrentfilename)
        else:
		tdef = newTdef        
        print >>sys.stderr,"main: Starting download, infohash is",`tdef.get_infohash()`
        
        # Select which video to play (if multiple)
        videofiles = tdef.get_files(exts=videoextdefaults)
        print >>sys.stderr,"main: Found video files",videofiles
        
        if len(videofiles) == 0:
            print >>sys.stderr,"main: No video files found! Let user select"
            # Let user choose any file
            videofiles = tdef.get_files(exts=None)
            
        if len(videofiles) > 1:
            selectedvideofile = self.ask_user_to_select_video(videofiles)
            if selectedvideofile is None:
                print >>sys.stderr,"main: User selected no video"
                return False
            dlfile = selectedvideofile
        else:
            dlfile = videofiles[0]
        
        # Free diskspace, if needed
        destdir = self.get_default_destdir()
        if not os.access(destdir,os.F_OK):
            os.mkdir(destdir)

        # Arno: For extra robustness, ignore any errors related to restarting
        # TODO: Extend code such that we can also delete files from the 
        # disk cache, not just Downloads. This would allow us to keep the
        # parts of a Download that we already have, but that is being aborted
        # by the user by closing the video window. See remove_current_*
        try:
            if not self.free_up_diskspace_by_downloads(tdef.get_infohash(),tdef.get_length([dlfile])):
                print >>sys.stderr,"main: Not enough free diskspace, ignoring"
                #return False
                pass # Let it slide
        except:
            print_exc()
        
        # Setup how to download
        dcfg = DownloadStartupConfig()
        dcfg.set_video_event_callback(self.sesscb_vod_event_callback)
        dcfg.set_video_events([VODEVENT_START,VODEVENT_PAUSE,VODEVENT_RESUME])
        dcfg.set_dest_dir(destdir)
        dcfg.set_max_speed(UPLOAD, MOJOMAX_UPLOAD)
        dcfg.set_max_speed(DOWNLOAD, MOJOMAX_DOWNLOAD)

        x.update("MAXUP", dcfg.get_max_speed(UPLOAD))
        x.update("MAXDOWN", dcfg.get_max_speed(DOWNLOAD))
        
        if tdef.is_multifile_torrent():
            dcfg.set_selected_files([dlfile])
        
        # Arno: 2008-7-15: commented out, just stick with old ABC-tuned 
        # settings for now
        #dcfg.set_max_conns_to_initiate(300)
        #dcfg.set_max_conns(300)
        
        # Start download
        dlist = self.s.get_downloads()
        infohash = tdef.get_infohash()

        # Start video window if not open
        #if self.videoFrame is None:
            #self.start_video_frame()
        #else:
            # Stop playing
            #self.videoplay.stop_playback()
        self.decodeprogress = 0
            
        self.videoFrame.videopanel.updateProgressSlider([False])
        
        # Stop all - not anymore hekhek
        newd = None
        for d in dlist:
            if d.get_def().get_infohash() == infohash:
                # Download already exists.
                # One safe option is to remove it (but not its downloaded content)
                # so we can start with a fresh DownloadStartupConfig. However,
                # this gives funky concurrency errors and could prevent a
                # Download from starting without hashchecking (as its checkpoint
                # was removed) 
                # Alternative is to set VOD callback, etc. at Runtime:
                print >>sys.stderr,"main: Reusing old duplicate Download",`infohash`
                newd = d
            #d.stop()
        
        self.s.lm.h4xor_reset_init_conn_counter()
        
        self.dlock.acquire()
        try:
            self.playermode = DLSTATUS_DOWNLOADING
            if newd is None:
                print >>sys.stderr,"main: Starting new Download",`infohash`
                newd = self.s.start_download(tdef,dcfg)
                if not x.data["HELPING"][0]:
                    global origDownload
                    origDownload = newd
                else:
                    global helpedDownload
                    helpedDownload = newd
                    #origDownload.set_max_desired_speed(self, dcfg.get_max_speed(UPLOAD))
            else:
                newd.set_video_event_callback(self.sesscb_vod_event_callback)
                if tdef.is_multifile_torrent():
                    newd.set_selected_files([dlfile])
                print >>sys.stderr,"main: Restarting existing Download",`infohash`
                newd.restart()

            self.d = newd
        finally:
            self.dlock.release()
        
        print >>sys.stderr,"main: Saving content to",self.d.get_dest_files()
        
        print >>sys.stderr,"Hello: ", self.d.get_max_desired_speed(DOWNLOAD)
        cname = tdef.get_name_as_unicode()
        if len(videofiles) > 1:
            cname += u' - '+bin2unicode(dlfile)
        self.videoplay.set_content_name(u'Loading: '+cname)
        
        try:
            [mime,imgdata] = tdef.get_thumbnail()
            if mime is not None:
                f = StringIO(imgdata)
                img = wx.EmptyImage(-1,-1)
                img.LoadMimeStream(f,mime,-1)
                self.videoplay.set_content_image(img)
            else:
                self.videoplay.set_content_image(None)
        except:
            print_exc()

        return True

    def i2icallback(self,cmd,param):
        """ Called by Instance2Instance thread """
        
        print >>sys.stderr,"main: Another instance called us with cmd",cmd,"param",param
        
        if cmd == 'START':
            torrentfilename = None
            if param.startswith('http:'):
                # Retrieve from web 
                f = tempfile.NamedTemporaryFile()
                n = urllib2.urlopen(url)
                data = n.read()
                f.write(data)
                f.close()
                n.close()
                torrentfilename = f.name
            else:
                torrentfilename = param
                
            # Switch to GUI thread
            wx.CallAfter(self.remote_start_download,torrentfilename)
    
    def mjcallback(self, addr, msg):
        """ Called by MojoCommunication thread """
        global SERVER_IP

        # do what you want to do to the recieved message in the main thread. hekhek
        #print >>sys.stderr,"[MJ-Notif-Peer] Callback function in main received: ", msg
        if(msg == "[MOJO] disconnect"):
            #print >>sys.stderr,"ELIJAH hekhek: ", ipAddr
            self.clear_session_state()
            self.videoplay.stop_playback()
            self.OnExit()

        if msg.startswith('[download-tstream]'):
            temp = msg.split("XxX+XxX") 
            tdef = pickle.loads(temp[1])
            highpeers = pickle.loads(temp[2])
            #include midpeers
            lowpeers = pickle.loads(temp[3])
            bandwidthAllocUL = pickle.loads(temp[4])
            bandwidthAllocDL = pickle.loads(temp[5])
            
            x.update("HELPING", True)
            x.update("RFLAG", True)
            x.equate("HIGHPEERLIST", highpeers)
            x.equate("LOWPEERLIST", lowpeers)
            x.update("HELPEDUL", bandwidthAllocUL)
            x.update("HELPEDDL", bandwidthAllocDL)
            self.start_download("mojoTstream", tdef)
            #kickout mid peers
            #self.d.update_peerlist(None)
            #print >>sys.stderr, "Succesfully downloaded tstream: ", tstream

        if msg.startswith('[latencytest]'):
            #self.mojoReply(addr[0])
            MojoCommunicationClient(MJ_LISTENPORT,'[latencyrep]',addr[0])

        if msg.startswith('[maxspeed]'):
            reply = '[maxspeed]['+pickle.dumps(x.data["MAXUP"][0])+']['+pickle.dumps(x.data["MAXDOWN"][0])
            MojoCommunicationClient(MJ_LISTENPORT,reply,addr[0])

        #####################
        if msg.startswith('[uldl]'):
            temp = msg.split('][')
            if(int(temp[1]) == 1 and x.data["STILLH"][0] and x.is_existing("CURRUL1")):
                reply = '[uldl]['+pickle.dumps(x.data["CURRUL1"][0])+']['+pickle.dumps(x.data["CURRDL1"][0])
            else:
                reply = '[uldl]['+pickle.dumps(x.data["CURRUL"][0])+']['+pickle.dumps(x.data["CURRDL"][0])
            MojoCommunicationClient(MJ_LISTENPORT,reply,addr[0])
        if msg.startswith('[aac]'):
            temp = msg.split('][')
            if(int(temp[1]) == 1 and x.data["STILLH"][0] and x.is_existing("ULLOG1")):
                reply = '[aac]['+pickle.dumps(x.averageData("ULLOG1"))+']['+pickle.dumps(x.averageData("DLLOG1"))
            else:
                reply = '[aac]['+pickle.dumps(x.averageData("ULLOG"))+']['+pickle.dumps(x.averageData("DLLOG"))
            MojoCommunicationClient(MJ_LISTENPORT,reply,addr[0])
            x.delete("ULLOG")
            x.delete("DLLOG")

        if msg.startswith('[checksu]'):
            SERVER_IP = addr[0]

        if msg.startswith('[REALLOC]'):
            temp = msg.split('XxX+XxX')
            bandwidthAllocUL = pickle.loads(temp[1])
            bandwidthAllocDL = pickle.loads(temp[2])
            x.update("HELPEDUL", bandwidthAllocUL)
            x.update("HELPEDDL", bandwidthAllocDL)
            x.update("RFLAG", True)

        if msg.startswith('[GET-PCKT]'):
            pcktLoss = origDownload.get_packet_loss()
            reply = '[PACKET]['+ pickle.dumps(pcktLoss)
            MojoCommunicationClient(MJ_LISTENPORT,reply,addr[0])

        if msg.startswith('[GET-NUMMSG]'):
            numMsgs = origDownload.get_num_msgs()
            reply = '[NUMMSG]['+ pickle.dumps(numMsgs)
            MojoCommunicationClient(MJ_LISTENPORT,reply,addr[0])
    
    def remote_start_download(self,torrentfilename):
        """ Called by GUI thread """
        self.remove_current_download_if_not_complete()
        self.start_download(torrentfilename)

    def videoserver_error_callback(self,e,url):
        """ Called by HTTP serving thread """
        wx.CallAfter(self.videoserver_error_guicallback,e,url)
        
    def videoserver_error_guicallback(self,e,url):
        print >>sys.stderr,"main: Video server reported error",str(e)
        #    self.show_error(str(e))
        pass

    def videoserver_set_status_callback(self,status):
        """ Called by HTTP serving thread """
        wx.CallAfter(self.videoserver_set_status_guicallback,status)

    def videoserver_set_status_guicallback(self,status):
        if self.videoFrame is not None:
            self.videoFrame.set_player_status(status)
    
    def free_up_diskspace_by_downloads(self,infohash,needed):
        
        if DEBUG:
            print >> sys.stderr,"main: free_up: needed",needed,DISKSPACE_LIMIT
        if needed > DISKSPACE_LIMIT:
            # Not cleaning out whole cache for bigguns
            if DEBUG:
                print >> sys.stderr,"main: free_up: No cleanup for bigguns"
            return True 
        
        inuse = 0L
        timelist = []
        dlist = self.s.get_downloads()
        for d in dlist:
            hisinfohash = d.get_def().get_infohash()
            if infohash == hisinfohash:
                # Don't delete the torrent we want to play
                continue
            destfiles = d.get_dest_files()
            if DEBUG:
                print >> sys.stderr,"main: free_up: Downloaded content",`destfiles`
            
            dinuse = 0L
            for (filename,savepath) in destfiles:
                stat = os.stat(savepath)
                dinuse += stat.st_size
            inuse += dinuse
            timerec = (stat.st_ctime,dinuse,d)
            timelist.append(timerec)
            
        if inuse+needed < DISKSPACE_LIMIT:
            # Enough available, done.
            if DEBUG:
                print >> sys.stderr,"main: free_up: Enough avail",inuse
            return True
        
        # Policy: remove oldest till sufficient
        timelist.sort()
        if DEBUG:
            print >> sys.stderr,"main: free_up: Found",timelist,"in dest dir"
        
        got = 0L
        for ctime,dinuse,d in timelist:
            print >> sys.stderr,"main: free_up: Removing",`d.get_def().get_name_as_unicode()`,"to free up diskspace, t",ctime
            self.s.remove_download(d,removecontent=True)
            got += dinuse
            if got > needed:
                return True
        # Deleted all, still no space:
        return False
        
        
    def show_error(self,msg):
        dlg = wx.MessageDialog(None, msg, "SwarmPlayer Error", wx.OK|wx.ICON_ERROR)
        result = dlg.ShowModal()
        dlg.Destroy()
        
    def OnExit(self):
        print >>sys.stderr,"main: ONEXIT"
        self.shuttingdown = True
        self.remove_current_download_if_not_complete()

        # To let Threads in Session finish their business before we shut it down.
        time.sleep(2) 
        
        if self.s is not None:
            self.s.shutdown()
        
        if self.tbicon is not None:
            self.tbicon.RemoveIcon()
            self.tbicon.Destroy()

        ts = enumerate()
        for t in ts:
            print >>sys.stderr,"main: ONEXIT: Thread still running",t.getName(),"daemon",t.isDaemon()
        
        if not ALLOW_MULTIPLE:
            del self.single_instance_checker
        self.ExitMainLoop()
	sys.exit(1)

    def sesscb_states_callback(self,dslist):
        """ Called by Session thread """
        # Arno: delegate to GUI thread. This makes some things (especially
        #access control to self.videoFrame easier
        #self.gui_states_callback(dslist)
        
        # Arno: we want the prebuf stats every second, and we want the
        # detailed peerlist, needed for research stats. Getting them every
        # second may be too expensive, so get them every 10.
        #
        self.getpeerlistcount += 1
        getpeerlist = (self.getpeerlistcount % 10) == 0
        haspeerlist =  (self.getpeerlistcount % 10) == 1
        
        wx.CallAfter(self.gui_states_callback,dslist,haspeerlist)
        
        #print >>sys.stderr,"main: SessStats:",self.getpeerlistcount,getpeerlist,haspeerlist
        return (1.0,getpeerlist) 

    def gui_states_callback(self,dslist,haspeerlist):
        """ Called by *GUI* thread.
        CAUTION: As this method is called by the GUI thread don't to any 
        time-consuming stuff here! """
        
        #print >>sys.stderr,"main: Stats:"
        if self.shuttingdown:
            return
        
        # See which Download is currently playing
        self.dlock.acquire()
        playermode = self.playermode
        d = self.d
        # print >>sys.stderr,"Orig Download! upload:", origDownload.get_max_desired_speed(UPLOAD)
        
        if x.data["updatePeerlist"][0]:
            x.update("updatePeerlist", not helpedDownload.update_peerlist(x.data['HIGHPEERLIST'], x.data['LOWPEERLIST']))
        if(x.data["HELPING"][0] or x.data["RFLAG"][0]) :
            x.update("HELPING", False)
            x.update("STILLH", True)
            x.update("RFLAG", False)

            maxOrigUpload = MOJOMAX_UPLOAD
            # origDownload.get_max_desired_speed(UPLOAD);
            print >>sys.stderr, "MAX ORIG UPLOAD", maxOrigUpload
            if x.data["HELPEDUL"][0] >= maxOrigUpload:
                helpedDownload.set_max_desired_speed(UPLOAD,maxOrigUpload)
                adjust = 0
            else:
                helpedDownload.set_max_desired_speed(UPLOAD,x.data["HELPEDUL"][0])
                adjust = maxOrigUpload - helpedDownload.get_max_desired_speed(UPLOAD)
            print >>sys.stderr, "HELPED SWARM MAX UPLOAD", helpedDownload.get_max_desired_speed(UPLOAD) 
            origDownload.set_max_desired_speed(UPLOAD, adjust)
            print >>sys.stderr, "NEW ORIG SWARM MAX UPLOAD",origDownload.get_max_desired_speed(UPLOAD)
            
            x.update("updatePeerlist", True)
                
            maxOrigDownload = MOJOMAX_DOWNLOAD
            if x.data["HELPEDDL"][0] >= maxOrigDownload:
                helpedDownload.set_max_desired_speed(DOWNLOAD,maxOrigDownload)
                adjust = 0
            else:
                helpedDownload.set_max_desired_speed(DOWNLOAD,x.data["HELPEDDL"][0])
                adjust = maxOrigDownload - helpedDownload.get_max_desired_speed(DOWNLOAD)
            print >>sys.stderr, "HELPED SWARM MAX DOWNLOAD", helpedDownload.get_max_desired_speed(DOWNLOAD)
            origDownload.set_max_desired_speed(DOWNLOAD, adjust)
            print >>sys.stderr, "NEW ORIG SWARM MAX DOWNLOAD",origDownload.get_max_desired_speed(DOWNLOAD)
        #if(x.data["STILLH"][0]):

        self.dlock.release()

        totalspeed = {}
        totalspeed[UPLOAD] = 0.0
        totalspeed[DOWNLOAD] = 0.0
        totalhelping = 0

        # When not playing, display stats for all Downloads and apply rate control.
        if playermode == DLSTATUS_SEEDING:
            for ds in dslist:
                print >>sys.stderr,"main: Stats: Seeding: %s %.1f%% %s" % (dlstatus_strings[ds.get_status()],100.0*ds.get_progress(),ds.get_error())
            self.ratelimit_callback(dslist)
            
        # Calc total dl/ul speed and find DownloadState for current Download
        ds = None
        i = -1
        print >>sys.stderr, "Download List Length: ", len(dslist)
        for ds2 in dslist:
            if ds2.get_vod_prebuffering_progress() >= 1 and SERVER_IP is not None and x.data["SFLAG"][0]:
                MojoCommunicationClient(MJ_LISTENPORT,"[sudelay]",SERVER_IP)
                x.update("SFLAG", False)

            if ds2.get_download() == d:
                ds = ds2

            elif playermode == DLSTATUS_DOWNLOADING:
                print >>sys.stderr,"main: Stats: Waiting: %s %.1f%% %s" % (dlstatus_strings[ds2.get_status()],100.0*ds2.get_progress(),ds2.get_error())
            
            for dir in [UPLOAD,DOWNLOAD]:
                totalspeed[dir] += ds2.get_current_speed(dir)
            totalhelping += ds2.get_num_peers()
            
            global totalSpeedAll
            if(origDownload == ds2.get_download()):
                i = 0
            elif (helpedDownload == ds2.get_download()):
                i = 1
            totalSpeedAll[i] = {}
            totalSpeedAll[i][UPLOAD] = ds2.get_current_speed(UPLOAD)
            totalSpeedAll[i][MAXUPLOAD] = ds2.get_download().get_max_desired_speed(UPLOAD)
            totalSpeedAll[i][DOWNLOAD] = ds2.get_current_speed(DOWNLOAD)
            totalSpeedAll[i][MAXDOWNLOAD] = ds2.get_download().get_max_desired_speed(DOWNLOAD)
    
            #if ds.get_vod_prebuffering_progress() >= 1:
            #    self.d.set_flag()

        
        # Report statistics on all downloads to research server, every 10 secs
        '''
        if haspeerlist:
            try:
                for d in dslist:
                    #self.reporter.report_stat(d)
            except:
                print_exc()
        '''
        # Set systray icon tooltip. This has limited size on Win32!
        txt = 'SwarmPlayer\n\n'
        txt += 'DL: %.1f\n' % (totalspeed[DOWNLOAD])
        txt += 'UL:   %.1f\n' % (totalspeed[UPLOAD])
        txt += 'Helping: %d\n' % (totalhelping)
        #print >>sys.stderr,"main: ToolTip summary",txt
        self.OnSetSysTrayTooltip(txt)
        
        # No current Download        
        if ds is None:
            return
        elif playermode == DLSTATUS_DOWNLOADING:
            mjtime = datetime.datetime.now().time()
            mjpeers = ds.get_peerlist()
            ######################
            if(x.data["STILLH"][0] and len(totalSpeedAll) > 1):
                x.update("CURRUL1", totalSpeedAll[1][UPLOAD])
                x.update("CURRDL1", totalSpeedAll[1][DOWNLOAD])
                x.log("ULLOG1", totalSpeedAll[1][UPLOAD])
                x.log("DLLOG1", totalSpeedAll[1][DOWNLOAD])    

            x.update("CURRUL", totalSpeedAll[0][UPLOAD])
            x.update("CURRDL", totalSpeedAll[0][DOWNLOAD])
            x.log("ULLOG", totalSpeedAll[0][UPLOAD])
            x.log("DLLOG", totalSpeedAll[0][DOWNLOAD])

            #print >>sys.stderr, "[MJ-MAXSPEED]\t%s\t%s" % (x.data["MAXDOWN"][0], x.data["MAXUP"][0])
            #self.mjlog_data(ds)

        # If we're done playing we can now restart any previous downloads to 
        # seed them.
        if playermode != DLSTATUS_SEEDING and ds.get_status() == DLSTATUS_SEEDING:
            self.restart_other_downloads() # GUI UPDATE

        #self.restart_other_downloads()

        # cf. 25 Mbps cap to reduce CPU usage and improve playback on slow machines
        # Arno: on some torrents this causes VLC to fail to tune into the video
        # although it plays audio???
        #ds.get_download().set_max_speed(DOWNLOAD,1500) 


        # Don't display stats if there is no video frame to show them on.
        if self.videoFrame is None:
            return
        else:
            self.display_stats_in_videoframe(ds,totalhelping,totalspeed)
    
    def display_stats_in_videoframe(self,ds,totalhelping,totalspeed):
        # Display stats for currently playing Download
        
        videoplayer_mediastate = self.videoFrame.get_state()
        #print >>sys.stderr,"main: Stats: VideoPlayer state",videoplayer_mediastate
        
        logmsgs = ds.get_log_messages()
        logmsg = None
        if len(logmsgs) > 0:
            print >>sys.stderr,"main: Log",logmsgs[0]
            logmsg = logmsgs[-1][1]
            
        preprogress = ds.get_vod_prebuffering_progress()
        playable = ds.get_vod_playable()
        t = ds.get_vod_playable_after()
        
        #print >>sys.stderr,"main: playble",playable,"preprog",preprogress
        print >>sys.stderr,"main: ETA is",t,"secs"
        if t > float(2 ** 30):
            intime = "inf"
        elif t == 0.0:
            intime = "now"
        else:
            h, t = divmod(t, 60.0*60.0)
            m, s = divmod(t, 60.0)
            if h == 0.0:
                if m == 0.0:
                    intime = "%ds" % (s)
                else:
                    intime = "%dm:%02ds" % (m,s)
            else:
                intime = "%dh:%02dm:%02ds" % (h,m,s)
                
        #print >>sys.stderr,"main: VODStats",preprogress,playable

        if ds.get_status() == DLSTATUS_HASHCHECKING:
            genprogress = ds.get_progress()
            pstr = str(int(genprogress*100))
            msg = "Checking already downloaded parts "+pstr+"% done"
        elif ds.get_status() == DLSTATUS_STOPPED_ON_ERROR:
            msg = 'Error playing: '+str(ds.get_error())
        elif playable:
            if not self.said_start_playback:
                msg = "Starting playback..."
                
            if videoplayer_mediastate == MEDIASTATE_STOPPED and self.said_start_playback:
                
                #print >>sys.stderr,"MEDIA STATE STOOPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPED",videoplayer_mediastate
                
                
                #npeers = ds.get_num_peers()
                #npeerstr = str(npeers)
                if totalhelping == 0:
                    topmsg = u"Please leave the SwarmPlayer running, this will help other SwarmPlayer users to download faster."
                else:
                    topmsg = u"Helping "+str(totalhelping)+" SwarmPlayer users to download. Please leave it running in the background."
                    
                # Display this on status line
                # TODO: Show balloon in systray when closing window to indicate things continue there
                msg = ''
                
                # Display helping info on "content name" line.
                self.videoplay.set_content_name(topmsg)
            elif videoplayer_mediastate == MEDIASTATE_PLAYING:
                self.said_start_playback = True
                # It may take a while for VLC to actually start displaying
                # video, as it is trying to tune in to the stream (finding
                # I-Frame). Display some info to show that:
                #
                cname = ds.get_download().get_def().get_name_as_unicode()
                topmsg = u'Decoding: '+cname+' '+str(self.decodeprogress)+' s'
                self.decodeprogress += 1
                msg = ''
                # Display tuning in info on "content name" line.
                self.videoplay.set_content_name(topmsg)
            elif videoplayer_mediastate == MEDIASTATE_PAUSED:
                msg = "Buffering... " + str(int(100.0*preprogress))+"%"
            else:
                msg = ''
                
        elif preprogress != 1.0:
            pstr = str(int(preprogress*100))
            npeers = ds.get_num_peers()
            npeerstr = str(npeers)
            if npeers == 0 and logmsg is not None:
                msg = logmsg
            else:
                msg = "Prebuffering "+pstr+"% done, eta "+intime+'  (connected to '+npeerstr+' people)'
                
            try:
                d = ds.get_download()
                tdef = d.get_def()
                videofiles = d.get_selected_files()
                if len(videofiles) == 0:
                    videofile = None
                else:
                    videofile = videofiles[0]
                if tdef.get_bitrate(videofile) is None:
                    msg += '. This video may not playmsgproperly because its bitrate is unknown.'
            except:
                print_exc()
        else:
            msg = "Waiting for sufficient download speed... "+intime
            
        global ONSCREENDEBUG
        if msg == '' and ONSCREENDEBUG:
            maxuptxt = "maxUpload %.1f\n" % (self.d.get_max_desired_speed(UPLOAD))
            uptxt = "actualUpload %.1f\n" % (totalSpeedAll[0][UPLOAD])
            maxdowntxt = "maxDownload %.1f\n" % (self.d.get_max_desired_speed(DOWNLOAD))
            downtxt = "actualDownload %.1f\n" % (totalSpeedAll[0][DOWNLOAD])
            peertxt = "peer %d" % (totalhelping)
            extra = ''
            if(x.data["STILLH"][0] and len(totalSpeedAll) > 1):
                extra = ('\n----NEW COLLAB STATS----'
                        + '\nmaxUpload ' + str(totalSpeedAll[0][MAXUPLOAD]) 
                        + '\nactualUpload ' + str(totalSpeedAll[0][UPLOAD]) 
                        + '\nmaxDownload ' + str(totalSpeedAll[0][MAXDOWNLOAD]) 
                        + '\nactualDownload ' + str(totalSpeedAll[0][DOWNLOAD]) 
                        + '\n BANDWIDTH UL RECEIVED ' + str(x.data["HELPEDUL"][0])
                        + '\n BANDWIDTH DL RECEIVED ' + str(x.data["HELPEDDL"][0])
                        + '\nhelpedMaxUpload ' + str(totalSpeedAll[1][MAXUPLOAD])
                        + '\nhelpedActualUpload ' + str(totalSpeedAll[1][UPLOAD]) 
                        + '\nhelpedMaxDownload ' + str(totalSpeedAll[1][MAXDOWNLOAD])
                        + '\nhelpedActualDownload ' + str(totalSpeedAll[1][DOWNLOAD]) )
            msg = maxuptxt + uptxt + maxdowntxt + downtxt + peertxt + extra + '\n--------'

        if msg is not None:    
            self.videoFrame.set_player_status(msg)
        self.videoFrame.videopanel.updateProgressSlider(ds.get_pieces_complete()) 

        boolArray = ds.get_pieces_complete()
        mjtime = datetime.datetime.now().time()
        #if len(boolArray) != 0:
            # PACKET LOSS TEST
            #print >>sys.stderr,"[MJ-pieces]\t%s\tTrueCount:\t%d\tTotalCount:\t%d\tPercentage:\t%.5f" % (mjtime,boolArray.count(True),len(boolArray),(boolArray.count(True) + 0.0)/len(boolArray))
            #print >>sys.stderr,"[MJ-PACKETLOSS]\t%s" % (ds.get_packet_loss())

        # Toggle save button
        self.videoFrame.videopanel.enableSaveButton(ds.get_status() == DLSTATUS_SEEDING, self.save_video_copy)    
            
        if False: # Only works if the sesscb_states_callback() method returns (x,True)
            peerlist = ds.get_peerlist()
            print >>sys.stderr,"main: Connected to",len(peerlist),"peers"
            for peer in peerlist:
                print >>sys.stderr,"main: Connected to",peer['ip'],peer['uprate'],peer['downrate']

    def set_ratelimits(self):
        uploadrate = float(self.playerconfig['total_max_upload_rate'])
        print >>sys.stderr,"main: restart_other_downloads: Setting max upload rate to",uploadrate
        self.r.set_global_max_speed(UPLOAD,uploadrate)
        self.r.set_global_max_seedupload_speed(uploadrate)

    def ratelimit_callback(self,dslist):
        """ When the player is in seeding mode, limit the used upload to
        the limit set by the user via the options menu. 
        Called by *GUI* thread """
        if self.r is None:
            return

        # ARNOTEST
        return

        # Adjust speeds once every 4 seconds
        adjustspeeds = False
        if self.count % 4 == 0:
            adjustspeeds = True
        self.count += 1
        
        if adjustspeeds:
            self.r.add_downloadstatelist(dslist)
            self.r.adjust_speeds()

    def OnSetSysTrayTooltip(self,txt):         
        if self.tbicon is not None:
            self.tbicon.set_icon_tooltip(txt)

    def sesscb_vod_event_callback(self,d,event,params):
        """ Called by the Session when the content of the Download is ready or has to be paused
         
        Called by Session thread """
        wx.CallAfter(self.gui_vod_event_callback,d,event,params)

    def gui_vod_event_callback(self,d,event,params):

        print >>sys.stderr, "gui_vod_event_callback",d,event,params

        if event == VODEVENT_START:
            filename = params["filename"]
            mimetype = params["mimetype"]
            stream   = params["stream"]
            length   = params["length"]

            print >>sys.stderr,"main: VOD ready callback called",currentThread().getName(),"###########################################################",mimetype
    
            if filename:
                self.play_from_file(filename)
            else:
                # HACK: TODO: make to work with file-like interface
                videoserv = VideoHTTPServer.getInstance()
                videoserv.set_inputstream(mimetype,stream,length)
                self.play_from_stream()
        elif event == VODEVENT_PAUSE:
            self.videoFrame.videopanel.Pause()
            self.videoFrame.set_player_status("Buffering...")
        elif event == VODEVENT_RESUME:
            self.videoFrame.videopanel.Play()
            self.videoFrame.set_player_status("")

    def play_from_stream(self):
        """ Called by MainThread """
        print >>sys.stderr,"main: Playing from stream"
        self.videoplay.play_url('http://127.0.0.1:'+str(VIDEOHTTP_LISTENPORT)+'/')
    
    def play_from_file(self,filename):
        """ Called by MainThread """
        print >>sys.stderr,"main: Playing from file",filename
        self.videoplay.play_url(filename)


    def restart_other_downloads(self):
        """ Called by GUI thread """
        if self.shuttingdown:
            return
        print >>sys.stderr,"main: Restarting other downloads"
        try:
            self.dlock.acquire()
            self.playermode = DLSTATUS_SEEDING
            self.r = UserDefinedMaxAlwaysOtherwiseEquallyDividedRateManager()
            self.set_ratelimits()
        finally:
            self.dlock.release()

        dlist = self.s.get_downloads()
        for d in dlist:
            if d != self.d:
                d.set_mode(DLMODE_NORMAL) # checkpointed torrents always restarted in DLMODE_NORMAL, just make extra sure
                d.restart() 

    def load_playerconfig(self,state_dir):
        self.playercfgfilename = os.path.join(state_dir,'playerconf.pickle')
        self.playerconfig = None
        try:
            f = open(self.playercfgfilename,"rb")
            self.playerconfig = pickle.load(f)
            f.close()
        except:
            print_exc()
            self.playerconfig = {}
            self.playerconfig['total_max_upload_rate'] = DEFAULT_MAX_UPLOAD_SEED_WHEN_SEEDING # KB/s

    def save_playerconfig(self):
        try:
            f = open(self.playercfgfilename,"wb")
            pickle.dump(self.playerconfig,f)
            f.close()
        except:
            print_exc()
            
    def set_playerconfig(self,key,value):
        self.playerconfig[key] = value
        
        if key == 'total_max_upload_rate':
            try:
                self.set_ratelimits()
            except:
                print_exc()
    
    def get_playerconfig(self,key):
        return self.playerconfig[key]
    
    def clear_session_state(self):
        """ Try to fix SwarmPlayer """
        try:
            self.videoplay.stop_playback()
        except:
            print_exc()
        try:
            if self.s is not None:
                dlist = self.s.get_downloads()
                for d in dlist:
                    self.s.remove_download(d,removecontent=True)
        except:
            print_exc()
        time.sleep(1) # give network thread time to do stuff
        try:
                dldestdir = self.get_default_destdir()
                shutil.rmtree(dldestdir,True) # ignore errors
        except:
            print_exc()
        try:
                dlcheckpointsdir = os.path.join(self.s.get_state_dir(),STATEDIR_DLPSTATE_DIR)
                shutil.rmtree(dlcheckpointsdir,True) # ignore errors
        except:
            print_exc()
        try:
                cfgfilename = os.path.join(self.s.get_state_dir(),STATEDIR_SESSCONFIG)
                os.remove(cfgfilename)
        except:
            print_exc()

        self.s = None # HARD EXIT
        #self.OnExit()
        sys.exit(0) # DIE HARD 4.0
    
    def get_default_destdir(self):
        return os.path.join(self.s.get_state_dir(),'downloads')
    
    def remove_current_download_if_not_complete(self):
        print >>sys.stderr,"main: Removing current download if not complete"
        self.dlock.acquire()
        d = self.d
        self.d = None
        self.dlock.release()
        if d is not None:
            d.set_state_callback(self.sesscb_remove_current_callback)
        
    def sesscb_remove_current_callback(self,ds):
        """ Called by SessionThread """
        d = ds.get_download()
        if (ds.get_status() == DLSTATUS_DOWNLOADING and ds.get_progress() >= 0.9) or ds.get_status() == DLSTATUS_SEEDING:
            pass
        else:
            self.dlock.acquire()
            try:
                if self.s is not None:
                    print >>sys.stderr,"main: Removing incomplete download"
                    self.s.remove_download(d,removecontent=True)
            finally:
                self.dlock.release()
        
        return (-1.0,False)
        
    def save_video_copy(self):
        # Save a copy of current download to other location
        # called by gui thread
        t = Thread(target = self.save_callback)
        t.setName( "SwarmplayerSave"+t.getName() )
        t.setDaemon(True)
        t.start()
    
    def save_callback(self):
        # Save a copy of current download to other location
        # called by new thread from self.save_video_copy
        try:
            if sys.platform == 'win32':
                # Jelle also goes win32, find location of "My Documents"
                # see http://www.mvps.org/access/api/api0054.htm
                from win32com.shell import shell
                pidl = shell.SHGetSpecialFolderLocation(0,0x05)
                defaultpath = shell.SHGetPathFromIDList(pidl)
            else:
                defaultpath = os.path.expandvars('$HOME')
        except Exception, msg:
            defaultpath = ''
            print_exc()
        
        self.dlock.acquire()
        dest_files = self.d.get_dest_files()
        self.dlock.release()
        
        dest_file = dest_files[0] # only single file for the moment in swarmplayer
        dest_file_only = os.path.split(dest_file[1])[1]
        print >> sys.stderr, 'Defaultpath:', defaultpath, 'Dest:', dest_file
        dlg = wx.FileDialog(self.videoFrame, 
                            message = self.utility.lang.get('savemedia'), 
                            defaultDir = defaultpath, 
                            defaultFile = dest_file_only,
                            wildcard = self.utility.lang.get('allfileswildcard') + ' (*.*)|*.*', 
                            style = wx.SAVE)
        dlg.Raise()
        result = dlg.ShowModal()
        dlg.Destroy()
        
        if result == wx.ID_OK:
            path = dlg.GetPath()
            print >> sys.stderr, 'Path:', path
            print >> sys.stderr, 'Copy: %s to %s' % (dest_file[1], path)
            if sys.platform == 'win32':
                try:
                    import win32file
                    win32file.CopyFile(dest_file[1], path, 0) # do succeed on collision
                except:
                    shutil.copyfile(dest_file[1], path)
            else:
                shutil.copyfile(dest_file[1], path)
    
class LinuxSingleInstanceChecker:
    
    def __init__(self,basename):
        self.basename = basename

    def IsAnotherRunning(self):
        "Uses pgrep to find other <self.basename>.py processes"
        # If no pgrep available, it will always start tribler
        cmd = 'pgrep -fl "%s\.py" | grep -v pgrep' % (self.basename)
        progressInfo = commands.getoutput(cmd)
        numProcesses = len(progressInfo.split('\n'))
        #if DEBUG:
        #    print 'main: ProgressInfo: %s, num: %d' % (progressInfo, numProcesses)
        return numProcesses > 1
                
        
##############################################################
#
# Main Program Start Here
#
##############################################################
def run(params = None):
    if params is None:
        params = [""]
    
    if len(sys.argv) > 1:
        params = sys.argv[1:]
    
    #if 'debug' in params:
    global ONSCREENDEBUG
    ONSCREENDEBUG=True
    
    # Create single instance semaphore
    # Arno: On Linux and wxPython-2.8.1.1 the SingleInstanceChecker appears
    # to mess up stderr, i.e., I get IOErrors when writing to it via print_exc()
    #
    # TEMPORARILY DISABLED on Linux
    if sys.platform != 'linux2':
        single_instance_checker = wx.SingleInstanceChecker("swarmplayer-" + wx.GetUserId())
    else:
        single_instance_checker = LinuxSingleInstanceChecker("swarmplayer")

    #print "[StartUpDebug]---------------- 1", time()-start_time
    if not ALLOW_MULTIPLE and single_instance_checker.IsAnotherRunning():
        if params[0] != "":
            torrentfilename = params[0]
            i2ic = Instance2InstanceClient(I2I_LISTENPORT,'START',torrentfilename)
            time.sleep(1)
            return
        
    arg0 = sys.argv[0].lower()
    if arg0.endswith('.exe'):
        installdir = os.path.abspath(os.path.dirname(sys.argv[0]))
    else:
        installdir = os.getcwd()  

    # Launch first single instance
    app = PlayerApp(0, params, single_instance_checker, installdir)
    app.MainLoop()
    
    print >>sys.stderr,"Sleeping seconds to let other threads finish"
    time.sleep(2)

if __name__ == '__main__':
    run()

