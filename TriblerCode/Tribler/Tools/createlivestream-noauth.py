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
import datetime
import math
from traceback import print_exc
from threading import Condition

from Tribler.Core.API import *
import Tribler.Core.BitTornado.parseargs as parseargs

from Tribler.mjlogger import *

argsdef = [('name', '', 'name of the stream'),
           ('source', '-', 'source to stream (url, file or "-" to indicate stdin)'),
           ('destdir', '.','dir to save torrent (and stream)'),
           ('bitrate', (512*1024)/8, 'bitrate of the streams in bytes'),
           ('piecesize', 32768, 'transport piece size'),
           ('duration', '1:00:00', 'duration of the stream in hh:mm:ss format'),
           ('nuploads', 7, 'the max number of peers to serve directly'),
           ('port', 7764, 'the TCP+UDP listen port'),
           ('thumb', '', 'filename of image in JPEG format, preferably 171x96')]

x = MJLogger()
x.log("TIME", time.time())
twin = 15.0
numhelp = 5

def state_callback(ds):
    d = ds.get_download()
    # MENMA EX
    mjtime = datetime.datetime.now().time()
    print >>sys.stderr, "[MJ-ServerStats]\t%s\t%s\t%s\t%.1f\t%s\tup\t%.1f\tdown\t%.1f" % (mjtime,`d.get_def().get_name()`,dlstatus_strings[ds.get_status()],ds.get_progress(),ds.get_error(),ds.get_current_speed(UPLOAD),ds.get_current_speed(DOWNLOAD))

    mjpeers = ds.get_peerlist()
    if mjpeers is not None :
        # ip, uprate, downrate, utotal, dtotal, speed
        for mjpeer in mjpeers:
            print >>sys.stderr,"[MJ-PL-spd]\t%s\t%s\t%s " % (mjtime, mjpeer['ip'], mjpeer['speed']/1024.0)
            print >>sys.stderr,"[MJ-PL-drur]\t%s\t%s\t%s\t%s" % (mjtime, mjpeer['ip'], mjpeer['downrate']/1024.0, mjpeer['uprate']/1024.0)
            print >>sys.stderr,"[MJ-PL-dtut]\t%s\t%s\t%s\t%s" % (mjtime, mjpeer['ip'], mjpeer['dtotal']/1024.0, mjpeer['utotal']/1024.0)
            
    mjlog_data(ds)
    if mjpeers is not None :
        mjcompute_criterion(ds)

    return (1.0,False)

def mjlog_data(ds):
    mjpeers = ds.get_peerlist()
    if mjpeers is not None :
        if(x.is_existing("PEERS")):
            x.delete("PEERS")

        for mjpeer in mjpeers:
            #utotal -> dtotal
            x.update("DT-"+str(mjpeer['id']),mjpeer['utotal']/1024.0)
            
            if(x.is_existing("PEERS")):
                if(mjpeer['id'] not in x.data["PEERS"]):
                    x.log("PEERS", mjpeer['id'])
            else:
                x.log("PEERS", mjpeer['id'])

            if(x.is_existing(mjpeer['id'])):
                x.update(mjpeer['id'], mjpeer['uprate']/1024.0)
            else:
                x.log(mjpeer['id'], mjpeer['uprate']/1024.0)

            x.log("AC-"+str(mjpeer['id']), mjpeer['uprate']/1024.0)

            #print >>sys.stderr, "[MJ-Log-PeerUpload]\t%s" % (x.data[mjpeer['id']])
            #print >>sys.stderr, "[MJ-AC-%s]\t%s" % (mjpeer['id'], x.data["AC-"+str(mjpeer['id'])])    

        if(x.is_existing("PEERS")):
            print >>sys.stderr, "[MJ-Log-Peers]\t%s" % (x.data["PEERS"])
       
def mjcompute_criterion(ds):
    mjpeers = ds.get_peerlist()
    
    #CRI
    if(x.is_existing("PEERS")):
        totalUpload = ds.get_current_speed(UPLOAD)
        for mjpeer in x.data["PEERS"]:
            totalUpload = totalUpload + float(x.data[mjpeer][0])

        peercount = len(x.data["PEERS"])
        CRI = totalUpload/(peercount*512)
        print >>sys.stderr,"[MJ-CRI-bit512]\t%f" % (CRI)

        #AC
        #add boundary for observing window (wrt time) for each peer
        #average all AC
        aac = False
        print >>sys.stderr,"[MJ-TIME]\t%f" % (time.time() - float(x.data["TIME"][0]))
        if(time.time() - float(x.data["TIME"][0]) >= twin):
            for mjpeer in x.data["PEERS"]:
                x.update("AAC-"+str(mjpeer), 0.0)
                for mjpeerup in x.data["AC-"+str(mjpeer)]:
                    x.update("AAC-"+str(mjpeer), float(x.data["AAC-"+str(mjpeer)][0]) + float(mjpeerup))
                x.update("AAC-"+str(mjpeer), float(x.data["AAC-"+str(mjpeer)][0])/twin)
                x.delete("AC-"+str(mjpeer))
                print >>sys.stderr, "[MJ-AAC-%s]\t%f" % (mjpeer, float(x.data["AAC-"+str(mjpeer)][0]))
            x.update("TIME", time.time())
            aac = True
            
        if(x.is_existing("AACFLAG")):
            if(aac):
                x.update("AACFLAG", True)
        else:
            x.log("AACFLAG", False)

        #ATFT
        if(x.is_existing("AAC-RANKED")):
            x.delete("AAC-RANKED")
        if(x.is_existing("DT-RANKED")):
            x.delete("DT-RANKED") 
        if(x.is_existing("FIN-RANKED")):
            x.delete("FIN-RANKED")   

        #RANK PEERS ACCORDING TO AAC IS POSSIBLE
        if(x.data["AACFLAG"][0]):
            ranked = []
            for mjpeer in x.data["PEERS"]:
                ranked.append(float(x.data["AAC-"+str(mjpeer)][0]))
            ranked = sorted(ranked, reverse=True)

            peerrank = []
            for mjpeerup in ranked:
                for mjpeer in x.data["PEERS"]:
                    if(float(x.data["AAC-"+str(mjpeer)][0]) == float(mjpeerup)):
                        mjpeerup = -1
                        peerrank.append(mjpeer)

            for mjpeer in peerrank:
                x.log("AAC-RANKED", mjpeer)

            print >>sys.stderr, "[MJ-AAC-RANKED]\t%s" % (x.data["AAC-RANKED"])

        #RANK PEERS ACCORDING TO TOTAL DOWNLOADED DATA FROM PEER
        if mjpeers is not None :
            ranked = []
            for mjpeer in x.data["PEERS"]:
                ranked.append(float(x.data["DT-"+str(mjpeer)][0]))
                print >>sys.stderr, "[DT-%s]\t%s" % (mjpeer, x.data["DT-"+str(mjpeer)][0])
            ranked = sorted(ranked, reverse=True)

            peerrank = []
            for mjpeerup in ranked:
                for mjpeer in x.data["PEERS"]:
                    if(float(x.data["DT-"+str(mjpeer)][0]) == float(mjpeerup)):
                        mjpeerup = -1
                        peerrank.append(mjpeer)

            for mjpeer in peerrank:
                x.log("DT-RANKED", mjpeer)

            print >>sys.stderr, "[MJ-DT-RANKED]\t%s" % (x.data["DT-RANKED"])

        #COMPARE RANKINGS
        if(x.data["AACFLAG"][0]):
            ranked1 = []
            ranked2 = []
            torank = []
            finranked = []

            for i in range(0, peercount):
                ranked1.append(i+1)

            for mjpeer in x.data["AAC-RANKED"]:
                ranked2.append(x.data["DT-RANKED"].index(mjpeer)+1)

            for i in range(0, peercount):
                torank.append(math.fabs(ranked1[i] - ranked2[i]))
            finranked = sorted(torank)

            print >>sys.stderr, "ranked1\t%s\nranked2\t%s" % (ranked1, ranked2) 
            aacranked = x.data["AAC-RANKED"]
            for i in range(0, peercount):
                for j in range(0, len(torank)):
                    if(finranked[i] == torank[j]):
                        torank[j] = -1
                        finranked[i] = aacranked[j]

            for mjpeer in finranked:
                x.log("FIN-RANKED", mjpeer)
        else:
            for mjpeer in x.data["DT-RANKED"]:
                x.log("FIN-RANKED", mjpeer)
            
        print >>sys.stderr, "[MJ-FIN-RANKED]\t%s" % (x.data["FIN-RANKED"]) 

        if(CRI < 1):
            helpingpeers = []
            if(numhelp >= peercount):
                helpingpeers = x.data["FIN-RANKED"]
            else:
                finranked = x.data["FIN-RANKED"]
                for i in range(0, numhelp):
                    helpingpeers.append(finranked[i])

            print >>sys.stderr,"SWARM NEEDS HELP PEERS TO HELP:\t%s" % (helpingpeers)

def vod_ready_callback(d,mimetype,stream,filename):
    """ Called by the Session when the content of the Download is ready
     
    Called by Session thread """
    print >>sys.stderr,"main: VOD ready callback called ###########################################################",mimetype

def get_usage(defs):
    return parseargs.formatDefinitions(defs,80)
    

if __name__ == "__main__":

    config, fileargs = parseargs.parseargs(sys.argv, argsdef, presets = {})
    print >>sys.stderr,"config is",config
    print "fileargs is",fileargs
    
    if config['name'] == '':
        print "Usage:  ",get_usage(argsdef)
        sys.exit(0)
        
    
    print "Press Ctrl-C to stop the download"

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


    # no LIVESOURCEAUTH for Pioneer, saves CPU


    tdef = TorrentDef()
    # hint: to derive bitrate and duration from a file, use
    #    ffmpeg -i file.mpeg /dev/null
    tdef.create_live(config['name'],config['bitrate'],config['duration'])
    tdef.set_tracker(s.get_internal_tracker_url())
    tdef.set_piece_length(config['piecesize']) #TODO: auto based on bitrate?
    if len(config['thumb']) > 0:
        tdef.set_thumbnail(config['thumb'])
    tdef.finalize()
    
    torrentbasename = config['name']+'.tstream'
    torrentfilename = os.path.join(config['destdir'],torrentbasename)
    tdef.save(torrentfilename)

    #tdef2 = TorrentDef.load(torrentfilename)
    #print >>sys.stderr,"main: Source auth pubkey2",`tdef2.metainfo['info']['live']`

    dscfg = DownloadStartupConfig()
    dscfg.set_dest_dir(config['destdir'])
    dscfg.set_live_aux_seeders( [
	    # servers: 
	    ("130.161.211.232",8764), # ss1
	    ("130.161.211.233",8764), # ss2
	    ("130.161.211.234",8764), # ss3
	    ("193.29.139.222",8764), # peer2peer.colo.mediadesign.nl, old ip
	    ("83.96.143.114",8764), # peer2peer.colo.mediadesign.nl, new ip
	    ("130.37.198.236",8764), # jip

	    # machines to verify the video stream:
	    ("130.161.159.89",8620), # jd's laptop 
	    ("130.161.159.210",8620), # arno's st interface
	    ] )
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
        
    dscfg.set_video_source(source)

    dscfg.set_max_uploads(config['nuploads'])
    # MENMA EX
    dscfg.set_max_speed(UPLOAD, 90)

    d = s.start_download(tdef,dscfg)
    d.set_state_callback(state_callback,getpeerlist=True)
   
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
    
