# Written by Arno Bakker 
# see LICENSE.txt for license information
#

import sys
import os
import shutil
import time
import tempfile
import random
import urllib2
import wx
from traceback import print_exc
from threading import Condition

from Tribler.Core.API import *
import Tribler.Core.BitTornado.parseargs as parseargs

from Tribler.mjlogger import *
from Tribler.Utilities.MojoCommunication import *

MJ_LISTENPORT = 6969
sendTstream = 0
tdef = None

argsdef = [('name', '', 'name of the stream'),
           ('source', '-', 'source to stream (url, file or "-" to indicate stdin)'),
           ('destdir', '.','dir to save torrent (and stream)'),
           ('bitrate', (512*1024)/8, 'bitrate of the streams in bytes'),
           ('piecesize', 32768, 'transport piece size'),
           ('duration', '1:00:00', 'duration of the stream in hh:mm:ss format'),
           ('nuploads', 7, 'the max number of peers to serve directly'),
           ('port', 7764, 'the TCP+UDP listen port'),
           ('thumb', '', 'filename of image in JPEG format, preferably 171x96')]

def state_callback(ds):
    global sendTstream
    d = ds.get_download()
   
    #print >>sys.stderr, "MOJO Peerlist: ", ds.get_peerlist()
    MOJOpeerlist = ds.get_peerlist()
    #if len(MOJOpeerlist) > 0:
    #    sendTstream = sendTstream + 1
    #print >>sys.stderr, "tstream: ", sendTstream 
    #if sendTstream == 30:
    #    for peer in MOJOpeerlist:
    #        sendMojoTstream(peer['ip'])
    
    print >>sys.stderr,`d.get_def().get_name()`,dlstatus_strings[ds.get_status()],ds.get_progress(),"%",ds.get_error(),"up",ds.get_current_speed(UPLOAD),"down",ds.get_current_speed(DOWNLOAD)

    return (1.0,False)

def vod_ready_callback(d,mimetype,stream,filename):
    """ Called by the Session when the content of the Download is ready
     
    Called by Session thread """
    print >>sys.stderr,"main: VOD ready callback called ###########################################################",mimetype

def get_usage(defs):
    return parseargs.formatDefinitions(defs,80)

def mjcallback(addr, msg):
    '''
    MOJO Server TODO, X => DONE
    [X] 1. If a HELP request is received, get the peerlist and torrent definition associated with it 
    [ ] 2. Call the createTorrentDef() to update the torrent definition to be user by sendMojoTstream()
    [ ] 2. Using the peerlist, get the peers that have the lowest absolute contribution.
           How to know how high is high and how low is low?
    [ ] 3. Instruct your peers with the highest absolute contribution to connect to the swarm that needs help
           by calling the function sendMojoTstream(ipAddr)
    [X] 4. Acknowledge and reply to the swarm that needs help with your peerlist
    '''
    
    if msg.startswith('[HELP] '):
        peerList = msg[19:]
        helpedPeerList = pickle.loads(peerList)
        # Get the peers with lowest absCon
        
        # For each helping peers, call the function sendMojoTstream with their IP address as arguments
        # sendMojoTstream(ipAddr)
        
        # Reply to the helped swarm with your peer list
        MojoCommunicationClient(MJ_LISTENPORT,'[ACK-HELP] ' + pickle.dumps(ds.get_peerlist()), addr)

def getHelp(ipAddr):    
    '''
    MOJO Server TODO, X => DONE
    [ ] 1. Mechanism for finding the helping swarm. For now, helping swarm is hard-coded
    [X] 2. Send a help request/message along with peerlist and torrent definition
    [ ] 3. Helping swarm will reply with its peerlist. Helped swarm should act accordingly.
    '''
    
    print >>sys.stderr,"Finding other swarms that can help..."
    helpingSwarmIP = "192.100.41.20" 
    # After some time
    print >>sys.stderr,"Helping swarm found. Initiating connection." 
    MojoCommunicationClient(MJ_LISTENPORT,'[HELP] ' + pickle.dumps(ds.get_peerlist()), helpingSwarmIP)
    
def sendMojoTstream(ipAddr):
    """ Called by MojoCommunication thread """
    print >>sys.stderr,"Sending tstream... ", ipAddr
    createTorrentDef()
    MojoCommunicationClient(MJ_LISTENPORT,'[download-tstream] ' + pickle.dumps(tdef), ipAddr)
    print >>sys.stderr,"MOJO"
    print >>sys.stderr,"MOJO"
    print >>sys.stderr,"MOJO"
    print >>sys.stderr,"MOJO"
    print >>sys.stderr,"MOJO"
    print >>sys.stderr,"MOJO"
    print >>sys.stderr,"MOJO"
    print >>sys.stderr,"MOJO"
    print >>sys.stderr,"MOJO"
    print >>sys.stderr,"MOJO"
    print >>sys.stderr,"MOJO"
    
def createTorrentDef():
    global tdef
    config['name'] = 'BaselineTest.mpegts'
    config['piecesize'] = 32768
    config['destdir'] = '.'
    config['source'] = 'http://10.40.87.177:8080/'
    config['nuploads'] = 7
    config['duration'] = '1:00:00'
    config['bitrate'] = 65536
    config['port'] = 7764
    config['thumb'] = ''
    
    authfilename = os.path.join(config['destdir'],config['name']+'.sauth')
    try:
        authcfg = ECDSALiveSourceAuthConfig.load(authfilename)
    except:
        print_exc()
        authcfg = ECDSALiveSourceAuthConfig()
        authcfg.save(authfilename)

    print >>sys.stderr,"main: Source auth pubkey",`str(authcfg.get_pubkey())`

    tdef = TorrentDef()
    # hint: to derive bitrate and duration from a file, use
    #    ffmpeg -i file.mpeg /dev/null
    tdef.create_live(config['name'],config['bitrate'],config['duration'],authcfg)
    tdef.set_tracker('http://10.40.87.177:7764/announce/')
    tdef.set_piece_length(config['piecesize']) #TODO: auto based on bitrate?
    if len(config['thumb']) > 0:
        tdef.set_thumbnail(config['thumb'])
    tdef.finalize()
    
    torrentbasename = config['name']+'.tstream'
    torrentfilename = os.path.join(config['destdir'],torrentbasename)
    tdef.save(torrentfilename)

if __name__ == "__main__":
    # global tdef
    # mjl = MJLogger()
    # mjl.log("Main", (1000, 12345))
    # mjl.log("Main", (2000, 4421, "Who?"))
    # mjl.debuglog("Main")

    config, fileargs = parseargs.parseargs(sys.argv, argsdef, presets = {})
    print >>sys.stderr,"config is",config
    print "fileargs is",fileargs
    
    if config['name'] == '':
        print "Usage:  ",get_usage(argsdef)
        sys.exit(0)
        
    
    print "Press Ctrl-C to stop the download"

    # Start server for MojoCommunication
    mojoServer = MojoCommunicationServer(MJ_LISTENPORT,mjcallback) 
    mojoServer.start()
            
    try:
        os.remove(os.path.join(config['destdir'],config['name']))
    except:
        print_exc()
    
    sscfg = SessionStartupConfig()
    statedir = tempfile.mkdtemp()
    sscfg.set_state_dir(statedir)
    sscfg.set_listen_port(config['port'])
    sscfg.set_megacache(False)
    sscfg.set_overlay(False)
    sscfg.set_dialback(True)
    
    s = Session(sscfg)


    # LIVESOURCEAUTH
    authfilename = os.path.join(config['destdir'],config['name']+'.sauth')
    try:
        authcfg = ECDSALiveSourceAuthConfig.load(authfilename)
    except:
        print_exc()
        authcfg = ECDSALiveSourceAuthConfig()
        authcfg.save(authfilename)

    print >>sys.stderr,"main: Source auth pubkey",`str(authcfg.get_pubkey())`
    print >>sys.stderr, "IP address PIPE: ", s.get_external_ip()

    tdef = TorrentDef()
    # hint: to derive bitrate and duration from a file, use
    #    ffmpeg -i file.mpeg /dev/null
    tdef.create_live(config['name'],config['bitrate'],config['duration'],authcfg)
    tdef.set_tracker(s.get_internal_tracker_url())
    tdef.set_piece_length(config['piecesize']) #TODO: auto based on bitrate?
    if len(config['thumb']) > 0:
        tdef.set_thumbnail(config['thumb'])
    tdef.finalize()
    
    torrentbasename = config['name']+'.tstream'
    torrentfilename = os.path.join(config['destdir'],torrentbasename)
    tdef.save(torrentfilename)
    #print >>sys.stderr,"Mojo Torrent Definition", tdef
    #tdef2 = TorrentDef.load(torrentfilename)
    #print >>sys.stderr,"main: Source auth pubkey2",`tdef2.metainfo['info']['live']`

    dscfg = DownloadStartupConfig()
    dscfg.set_dest_dir(config['destdir'])

    if config['source'] == '-':
        # Arno: doesn't appear to work on Linux
        source = sys.stdin
    elif config['source'].startswith('http:'):
        # HTTP source
        source = urllib2.urlopen(config['source'])
        """
        # Windows Media Encoder gives Bad Request if we don't include User-Agent
        url = config['source']
        user_agent = 'NSPlayer/4.1.0.3856'
        headers = { 'User-Agent' : user_agent }

        req = urllib2.Request(url, None, headers)
        source = urllib2.urlopen(req)
        """
    elif config['source'].startswith('pipe:'):
        # Program as source via pipe
        cmd = config['source'][len('pipe:'):]
        (child_out,source) = os.popen2( cmd, 'b' )
    else:
        # File source
        source = open(config['source'],"rb")
        dscfg.set_video_ratelimit(tdef.get_bitrate())
        
    dscfg.set_video_source(source,authcfg)

    dscfg.set_max_uploads(config['nuploads'])
    # MENMA EX
    dscfg.set_max_speed(UPLOAD, 1000000)
    
    # limit the # of connections to the server to only ONE peer so that other peers will connect to each other and not to server only
    # change this later so that number of connected peers  = totalServerUpload/bitrate
    # MOJO - tested and working hekhek
    # dscfg.set_max_conns_to_initiate(1)
    dscfg.set_max_conns(1)

    d = s.start_download(tdef,dscfg)
    d.set_state_callback(state_callback,getpeerlist=True)
   
    '''
    MOJO Server TODO, X => DONE
    [ ] 1. Compute for the CIRI periodically
    [ ] 2. Record the absolute contribute for each peer
    [ ] 3. When the CIRI of the swarm becomes less than 1, call the function getHelp()
    '''
    
    # prompt the user where to connect
    ex = wx.App()
    ex.MainLoop()
    cont = True
    while cont:
        dialog = wx.TextEntryDialog(None, "Input IP Address of peer you want to connect to the other swarm","MojoCommunication", "127.0.0.1", style=wx.OK|wx.CANCEL)
        if dialog.ShowModal() == wx.ID_OK:
            print >>sys.stderr, "You entered: %s" % dialog.GetValue()
            sendMojoTstream(dialog.GetValue())
        else :
            cont = False

    #dialog.Destroy()
    # condition variable would be prettier, but that don't listen to 
    # KeyboardInterrupt
    time.sleep(sys.maxint/2048)
    try:
        while True:
            x = sys.stdin.read()
    except:
        print_exc()
    cond = Condition()
    cond.acquire()
    cond.wait()
    
    s.shutdown()
    time.sleep(3)    
    shutil.rmtree(statedir)