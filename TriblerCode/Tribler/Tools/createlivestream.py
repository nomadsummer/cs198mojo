# Written by Arno Bakker 
# see LICENSE.txt for license information
#

import sys
import os
import shutil
import time
import datetime
import tempfile
import random
import urllib2
import datetime
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

x = MJLogger()
x.log("TIME", time.time())
x.log("STARTTIME", float(x.data["TIME"][0]))
x.log("BANDCOUNT", 1)
x.log("LATCOUNT", 0)
x.log("LATCHECK", 0)
x.log("HELPED", False)
x.log("HELPING", True)
twin = 15.0

x.log("PACKETLOSS", 0.0)
x.log("CONTINDEX", 0.0)
x.log("BANDUTIL", 0.0)
x.log("AVGLATENCY", 0.0)

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
    
    # MENMA EX
    mjtime = datetime.datetime.now().time()
    #print >>sys.stderr, "[MJ-ServerStats]\t%s\t%s\t%s\t%.1f\t%s\tup\t%.1f\tdown\t%.1f" % (mjtime,`d.get_def().get_name()`,dlstatus_strings[ds.get_status()],ds.get_progress(),ds.get_error(),ds.get_current_speed(UPLOAD),ds.get_current_speed(DOWNLOAD))

    mjpeers = ds.get_peerlist()
    #if len(mjpeers) > 0:
        # ip, uprate, downrate, utotal, dtotal, speed
        #for mjpeer in mjpeers:
            #print >>sys.stderr,"[MJ-PL-spd]\t%s\t%s\t%s " % (mjtime, mjpeer['ip'], mjpeer['speed']/1024.0)
            #print >>sys.stderr,"[MJ-PL-drur]\t%s\t%s\t%s\t%s" % (mjtime, mjpeer['ip'], mjpeer['downrate']/1024.0, mjpeer['uprate']/1024.0)
            #print >>sys.stderr,"[MJ-PL-dtut]\t%s\t%s\t%s\t%s" % (mjtime, mjpeer['ip'], mjpeer['dtotal']/1024.0, mjpeer['utotal']/1024.0)
    
    # START        
    mjlog_data(ds)
    if len(mjpeers) > 0:
        mjcompute_criterion(ds)

    # LATENCY
    MOJOpeerlist = ds.get_peerlist()
    print >>sys.stderr, "[MJ-Peers]\t", len(MOJOpeerlist)
    if(len(MOJOpeerlist) > 0 and float(x.data["LATCOUNT"][0]) == 0):
        x.update("LATCOUNT", len(MOJOpeerlist))
        x.update("AVGLATENCY", 0.0)
        for mjpeer in MOJOpeerlist:
            if(x.is_existing(mjpeer['id'])):
                x.update("LATENCY-" + str(mjpeer['id']), time.time())
            else:
                x.log("LATENCY-" + str(mjpeer['id']), time.time())
            #SEND MESSAGE
            print >>sys.stderr, "[MJ-Peerip]:\t", mjpeer['ip']
            mojoLatencyTest(mjpeer['id'], mjpeer['ip'])

    if(float(x.data["LATCHECK"][0]) == float(x.data["LATCOUNT"][0]) and float(x.data["LATCOUNT"][0]) > 0):
        x.update("AVGLATENCY", x.data["AVGLATENCY"][0]/x.data["LATCOUNT"][0])
        print >>sys.stderr, "[MJ-Base-Latency]\t%s" % (x.data["AVGLATENCY"][0])
        x.update("LATCOUNT", 0)
        x.update("LATCHECK", 0)

    #PACKET LOSS
    completePieces = ds.get_pieces_complete()
    numTrue = completePieces.count(True)
    if(len(completePieces) > 0):
        print >>sys.stderr, "[MJ-DEBUG]\tTrue:\t%s\tPercent:\t%s" % (numTrue, ((float(numTrue)/float(len(completePieces)))*100))

    return (1.0,False)

def mjlog_data(ds):
    mjpeers = ds.get_peerlist()
    if len(mjpeers) > 0:
        if(x.is_existing("PEERS")):
            x.delete("PEERS")

        averageUp = 0.0
        totalUpload = ds.get_current_speed(UPLOAD)
        totalDownload = ds.get_current_speed(DOWNLOAD)

        for mjpeer in mjpeers:
            if(x.is_existing("PEERS")):
                if(mjpeer['id'] not in x.data["PEERS"]):
                    x.log("PEERS", mjpeer['id'])
            else:
                x.log("PEERS", mjpeer['id'])

            totalUpload = totalUpload + mjpeer['uprate']/1024.0
            totalDownload = totalDownload + mjpeer['downrate']/1024.0

            averageUp = averageUp + mjpeer['uprate']/1024.0

            if(x.is_existing(mjpeer['id'])):
                x.update(mjpeer['id'], mjpeer['uprate']/1024.0)
            else:
                x.log(mjpeer['id'], mjpeer['uprate']/1024.0)

            x.log("AC-"+str(mjpeer['id']), mjpeer['uprate']/1024.0)
            x.update("IP-"+str(mjpeer['id']), mjpeer['ip'])

            #print >>sys.stderr, "[MJ-Log-PeerUpload]\t%s" % (x.data[mjpeer['id']])
            #print >>sys.stderr, "[MJ-AC-%s]\t%s" % (mjpeer['id'], x.data["AC-"+str(mjpeer['id'])])    

        x.update("AvgUp", averageUp/len(x.data["PEERS"]))
        x.update("BANDUTIL", (totalUpload - totalDownload)/x.data["BANDCOUNT"][0])
        x.update("BANDCOUNT", x.data["BANDCOUNT"][0] + 1)

        #print >>sys.stderr, "[MJ-Base-BandUtil]\t%s" % (x.data["BANDUTIL"][0])

        #if(x.is_existing("PEERS")):
            #print >>sys.stderr, "[MJ-Log-Peers]\t%s" % (x.data["PEERS"])
            
        #for mjpeer in mjpeers:
            #print >>sys.stderr, "[MJ-Log-Peers-IP]\t%s" % (mjpeer['ip'])
   
def mjcompute_criterion(ds):
    mjpeers = ds.get_peerlist()
    
    #CIRI
    if(x.is_existing("PEERS")):
        if(x.is_existing("CIRI")):
            x.delete("CIRI")

        totalUpload = ds.get_current_speed(UPLOAD)
        peercount = len(x.data["PEERS"])
        if(peercount > 0):
            for mjpeer in x.data["PEERS"]:
                totalUpload = totalUpload + float(x.data[mjpeer][0])

            toParse = ds.get_videoinfo()
            bitRate = toParse['bitrate']
            x.update("CIRI", totalUpload/(peercount*bitRate))
            #print >>sys.stderr,"[MJ-CIRI]\t%f" % (x.data["CIRI"][0])

        #AC
        #add boundary for observing window (wrt time) for each peer
        #average all AC
        if(time.time() - float(x.data["TIME"][0]) >= twin or float(x.data["TIME"][0]) == float(x.data["STARTTIME"][0])):
            checktime = True
        else:
            checktime = False

        if(checktime):
            for mjpeer in x.data["PEERS"]:
                x.update("AAC-"+str(mjpeer), 0.0)
                for mjpeerup in x.data["AC-"+str(mjpeer)]:
                    x.update("AAC-"+str(mjpeer), float(x.data["AAC-"+str(mjpeer)][0]) + float(mjpeerup))
                x.update("AAC-"+str(mjpeer), float(float(x.data["AAC-"+str(mjpeer)][0])/len(x.data["AC-"+str(mjpeer)])))
                x.delete("AC-"+str(mjpeer))
                #print >>sys.stderr, "[MJ-AAC-%s]\t%f" % (mjpeer, float(x.data["AAC-"+str(mjpeer)][0]))
            x.update("TIME", time.time())

        if(x.is_existing("AAC-RANKED")):
            x.delete("AAC-RANKED")
        if(x.is_existing("HIGH-RANKED")):
            x.delete("HIGH-RANKED")   
        if(x.is_existing("LOW-RANKED")):
            x.delete("LOW-RANKED")   

        #RANK PEERS ACCORDING TO AAC IS POSSIBLE
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

        #print >>sys.stderr, "[MJ-AAC-RANKED]\t%s" % (x.data["AAC-RANKED"])

        for index in range(0, int(len(x.data["AAC-RANKED"])/5) + 1):
            x.log("HIGH-RANKED", x.data["AAC-RANKED"][index])
        
        #if(x.is_existing("HIGH-RANKED") and len(x.data["HIGH-RANKED"]) > 0):
            #print >>sys.stderr, "[MJ-HIGH-RANKED]\t%s" % (x.data["HIGH-RANKED"]) 

        for index in range(0, int(len(x.data["AAC-RANKED"])/5) + 1):
            x.log("LOW-RANKED", x.data["AAC-RANKED"][len(x.data["AAC-RANKED"])-1 - index])
            
        #if(x.is_existing("LOW-RANKED") and len(x.data["LOW-RANKED"]) > 0):
            #print >>sys.stderr, "[MJ-LOW-RANKED]\t%s" % (x.data["LOW-RANKED"]) 

        if(x.data["CIRI"][0] < 1):
            if(x.is_existing("highpeers")):
                x.delete("highpeers")   
            if(x.is_existing("lowpeers")):
                x.delete("lowpeers")

            if(x.is_existing("HIGH-RANKED") and len(x.data["HIGH-RANKED"]) > 0):
                for index in range(0, int(len(x.data["HIGH-RANKED"])/5) + 1):
                    hightemp = {}
                    hightemp['id'] = str(x.data["HIGH-RANKED"][index])
                    hightemp['ip'] = x.data["IP-"+str(x.data["HIGH-RANKED"][index])][0]
                    x.log("highpeers", hightemp)

            if(x.is_existing("LOW-RANKED") and len(x.data["LOW-RANKED"]) > 0):
                for index in range(0, int(len(x.data["LOW-RANKED"])/5) + 1):
                    lowtemp = {}
                    lowtemp['id'] = str(x.data["LOW-RANKED"][index])
                    lowtemp['ip'] = x.data["IP-"+str(x.data["LOW-RANKED"][index])][0]
                    x.log("lowpeers", lowtemp)

            print >>sys.stderr,"SWARM NEEDS HELP"
            print >>sys.stderr,"HIGHEST AAC:\t%s" % (x.data["highpeers"])
            print >>sys.stderr,"LOWEST AAC:\t%s" % (x.data["lowpeers"])
            
            print >>sys.stderr,"Calling the getHelp() function..."
            #getHelp(x.data["highpeers"], x.data["lowpeers"])

            mjbandwidth_allocation(ds)

def mjbandwidth_allocation(ds):
    if(x.is_existing("MIN-NEEDED")):
        x.delete("MIN-NEEDED")

    toParse = ds.get_videoinfo()
    bitRate = toParse['bitrate']
    peercount = len(x.data["PEERS"])
    minBandwidth = peercount*bitRate
    totalUpload = ds.get_current_speed(UPLOAD)
    if(peercount > 0):
        for mjpeer in x.data["PEERS"]:
            totalUpload = totalUpload + float(x.data[mjpeer][0])

    minBandwidth = minBandwidth - totalUpload

    x.log("MIN-NEEDED", minBandwidth)

    for mjpeer in x.data["PEERS"]:
        Beta = 1
        Alpha = 1
        leftSide = Beta*(float(x.data["AAC-"+str(mjpeer)][0]) - x.data["AvgUp"][0])
        rightSide = (1 - Beta)*(float(x.data["AAC-"+str(mjpeer)][0]))
        preTotal = leftSide + rightSide

        x.update("BA-"+str(mjpeer), Alpha*preTotal)
        #print >>sys.stderr, "[MJ-AVGUP]\t%f" % (float(x.data["AvgUp"][0]))
        #print >>sys.stderr, "[MJ-AAC-%s]\t%f" % (mjpeer, float(x.data["AAC-"+str(mjpeer)][0]))
        #print >>sys.stderr, "[MJ-BA-%s]\t%f" % (mjpeer, float(x.data["BA-"+str(mjpeer)][0]))

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
    print >>sys.stderr,"[MJ-Notif-Host] Callback function in main received: ", msg    
    print >>sys.stderr,"[MJ-Notif-Host]"
    print >>sys.stderr,"[MJ-Notif-Host]"
    print >>sys.stderr,"[MJ-Notif-Host]"
    print >>sys.stderr,"[MJ-Notif-Host]"
    print >>sys.stderr,"[MJ-Notif-Host]"
    print >>sys.stderr,"[MJ-Notif-Host]"
    print >>sys.stderr,"[MJ-Notif-Host]"
    print >>sys.stderr,"[MJ-Notif-Host]"
    print >>sys.stderr,"[MJ-Notif-Host]"
    print >>sys.stderr,"[MJ-Notif-Host]"
    print >>sys.stderr,"[MJ-Notif-Host]"

    if msg.startswith('[HELP]+'):
        temp = msg.split("+")
        helpedTorrentDef = pickle.loads(temp[1])
        helpedhighpeers = pickle.loads(temp[2])
        helpedlowpeers = pickle.loads(temp[3])
        # Get the peers with lowest absCon
        
        # For each helping peers, call the function sendMojoTstream with their IP address as arguments
        # sendMojoTstream(ipAddr)
        for mjpeer in  x.data["highpeers"]:
            sendMojoTstream(mjpeer['ip'], helpedTorrentDef)
        
        # Reply to the helped swarm with your peer list
        MojoCommunicationClient(MJ_LISTENPORT,'[ACK-HELP]+' + pickle.dumps(x.data["highpeers"]) + '+' + pickle.dumps(x.data["lowpeers"]), addr)
    elif msg.startswith('[latencyrep]'):
        strs = msg.split("][")
        peerid = strs[1]
        print >>sys.stderr,"[BEFORE]\t%s\t%s\t%s" % (x.data["LATENCY-"+peerid][0], x.data["AVGLATENCY"][0], x.data["LATCHECK"][0])
        x.update("LATENCY-" + peerid, time.time() - float(x.data["LATENCY-" + peerid][0]))
        x.update("AVGLATENCY", float(x.data["AVGLATENCY"][0]) + float(x.data["LATENCY-" + peerid][0]))
        x.update("LATCHECK", float(x.data["LATCHECK"][0]) + 1)
        print >>sys.stderr,"[AFTER]\t%s\t%s\t%s" % (x.data["LATENCY-"+peerid][0], x.data["AVGLATENCY"][0], x.data["LATCHECK"][0])


def getHelp(highpeers, lowpeers):    
    '''
    MOJO Server TODO, X => DONE
    [ ] 1. Mechanism for finding the helping swarm. For now, helping swarm is prompted
    [X] 2. Send a help request/message along with peerlist and torrent definition
    [ ] 3. Helping swarm will reply with its peerlist. Helped swarm should act accordingly.
    '''
    
    print >>sys.stderr,"Finding other swarms that can help..."
    
    '''
    # prompt the user where to connect
    ex = wx.App()
    ex.MainLoop()
    dialog = wx.TextEntryDialog(None, "Input IP Address of helping swarm","MojoCommunication", "127.0.0.1", style=wx.OK)
    if dialog.ShowModal() == wx.ID_OK:
        print >>sys.stderr, "You entered: %s" % dialog.GetValue()
        helpingSwarmIP = dialog.GetValue()
    '''
    
    helpingSwarmIP = "10.40.81.150"
    # After some time
    print >>sys.stderr,"Helping swarm found. Initiating connection." 
    #print >>sys.stderr,"orig tdef " + pickle.dumps(origTdef)
    MojoCommunicationClient(MJ_LISTENPORT,'[HELP]+' + pickle.dumps(origTdef) + '+ '+ pickle.dumps(highpeers) + '+' + pickle.dumps(lowpeers), helpingSwarmIP)
    
def sendMojoTstream(ipAddr, torrentdef):
    """ Called by MojoCommunication thread """
    print >>sys.stderr,"Sending tstream... ", ipAddr
    #createTorrentDef()
    MojoCommunicationClient(MJ_LISTENPORT,'[download-tstream] ' + pickle.dumps(torrentdef), ipAddr)
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

def mojoLatencyTest(peerid, ipAddr):
    # do what you want to do to the recieved message in the main thread. hekhek
    print >>sys.stderr,"Testing Latency... ", ipAddr
    #toPrint = '[latencytest]['+peerid+']['+s.get_external_ip()
    #print >>sys.stderr,"PRINT: ", toPrint
    MojoCommunicationClient(MJ_LISTENPORT,'[latencytest]['+peerid+']['+s.get_external_ip(), ipAddr)
    
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
    global origTdef
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
    origTdef = tdef
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
    [X] 1. Compute for the CIRI periodically
    [X] 2. Record the absolute contribution for each peer
    [X] 3. When the CIRI of the swarm becomes less than 1, call the function getHelp()
    '''
    
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
    '''

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