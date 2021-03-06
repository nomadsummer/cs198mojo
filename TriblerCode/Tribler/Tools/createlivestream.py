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

from Tribler.serverframe import *
from Tribler.mjlogger import *
from Tribler.Utilities.MojoCommunication import *

MJ_LISTENPORT = 6969
sendTstream = 0
tdef = None
counter = 0
#dGlobal = None

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
x.log("HELPED", False)
x.log("ONHELP", False)
twin = 15.0
graceInt = 0
flag = True
firstTime = True
checktime = False
collabInt = 0
helpingSwarmIP = ""

x.log("BUUP", 0.0)
x.log("BUDOWN", 0.0)
x.log("BUCOUNT", 0)
x.log("BUCHECK", 0)
x.log("LATCOUNT", 0)
x.log("GUILATENCY", 0)
x.log("GUIPACKET", 0)
x.log("GUIMSG", 0)
x.log("LATTIME", float(x.data["TIME"][0]))
x.log("ACTIME", float(x.data["TIME"][0]))
latInt = 15.0 #Do not make less than 15
timeout = latInt

dataFile = open("C:\\Temp\\Latency.txt", "w+")
dataFile2 = open("C:\\Temp\\BandUtil.txt", "w+")
dataFile3 = open("C:\\Temp\\CIRI.txt", "w+")
dataFile4 = open("C:\\Temp\\Extra.txt", "w+")
dataFile5 = open("C:\\Temp\\SUDelay.txt", "w+")
dataFile6 = open("C:\\Temp\\PacketLoss.txt", "w+")
dataFile7 = open("C:\\Temp\\MsgCount.txt", "w+")
dataFile8 = open("C:\\Temp\\MCIRI.txt", "w+")

#########
x.log("KLEN", 0)
x.log("KFLAG", True)
x.log("MLEN", 0)
x.log("MFLAG", True)
x.log("PCHECK", 0)
x.log("PLEN", 0)
x.log("PFLAG", True)
x.log("CCHECK", 0)
x.log("CLEN", 0)
x.log("CFLAG", True)
x.log("BFLAG", True)
x.log("LFLAG", True)
x.log("SUL", 0.0)
x.log("SDL", 0.0)
x.log("BRATE", 200.0)
x.init("PEERS")
x.init("PACKETLOSS")
x.init("MSGCOUNT")
x.init("AVGLATENCY")

MOJOMAXUPLOAD = 6000

def state_callback(ds):
    global sendTstream
    global graceInt
    global flag
    global firstTime
    global dataFile
    global dataFile2
    global dataFile3
    global dataFile4
    global dataFile5
    global dataFile6
    global dataFile7
    global dataFile8

    d = ds.get_download()
    
    msg = 'Server Statistics\n\n'
    
    #maximum server upload
    msg += 'Server Upload:\t' + str(d.get_max_desired_speed(UPLOAD)) + ' kbps\n'
    msg += 'Video Bitrate:\t' + str(x.data["BRATE"][0]) + '\n'
    status = "Normal"
    if(x.data["HELPED"][0]):
        status = "Helped"
    if(x.data["ONHELP"][0]):
        status = "Helping"
    msg += 'Server Status:\t' + status + '\n\n'
    #add something about actual server upload if there is such a thing....
    ciri = ""
    #mciri = ""
    netup = ""
    if(x.is_existing("CIRI")):
        ciri = x.data["CIRI"][0]
    if(x.is_existing("MCIRI")):
        ciri = x.data["MCIRI"][0]
    if(x.is_existing("NetUpCon")):
        netup = x.data["NetUpCon"][0]
    msg += 'CIRI:\t' + str(ciri) + '\n'
    #msg += 'MCIRI:\t' + str(mciri) + '\n'   
    msg += 'NetUpCon:\t' + str(netup) + '\n'
    msg += 'ServerUpload\t' + str(ds.get_current_speed(UPLOAD)) + "\n"
    msg += 'BandwidthUtil \tUp: ' + str(x.data["BUUP"][0]) + " Down: " + str(x.data["BUDOWN"][0]) + "\n"
    msg += 'AvgLatency\t' + str(x.data["GUILATENCY"][0]) + "\n"
    msg += 'PacketLoss\t' + str(x.data["GUIPACKET"][0]) + "\n"
    msg += 'AvgNumMsgs\t' + str(x.data["GUIMSG"][0]) + "\n"
    msg += '\nPEERLIST WITH AC RANKINGS\n-------------------------\n'
    count = 1 
    if(x.is_existing("AC-RANKED")):
        for mjpeer in x.data["AC-RANKED"]:
            if(x.is_existing("ACUL-"+str(mjpeer)) and x.is_existing("UL-"+str(mjpeer))):
                msg += str(count) + ") " + str(mjpeer) + ' AC: ' + str(x.data["ACUL-"+str(mjpeer)][0]) + ' Up: ' + str(x.data["UL-"+str(mjpeer)][0]) + ' Down: ' + str(x.data["DL-"+str(mjpeer)][0]) + '\n'
            count += 1
    top.set_player_status(msg)
    
    if(firstTime):
        print >>dataFile, "##\t##"
        print >>dataFile2, "##\t##\t##"
        #print >>dataFile2, "##\t##"
        print >>dataFile3, "##\t##"
        print >>dataFile5, "##\t##"
        print >>dataFile6, "##\t##"
        print >>dataFile7, "##\t##"
        print >>dataFile8, "##\t##\t##"

        print >>dataFile, "Time\tLatency"
        print >>dataFile2, "Time\tBandUtilUp\tBandUtilDown"
        #print >>dataFile2, "Time\tBandUtilUp"
        print >>dataFile3, "Time\tCIRI/MCIRI"
        print >>dataFile5, "Time\tSUDelay"
        print >>dataFile6, "Time\tPacketLoss"
        print >>dataFile7, "Time\tMsgCount"
        print >>dataFile8, "Time\tTotalUp\tNetUpCon"

        dataFile = open("C:\\Temp\\Latency.txt", "a+")
        dataFile2 = open("C:\\Temp\\BandUtil.txt", "a+")
        dataFile3 = open("C:\\Temp\\CIRI.txt", "a+")
        dataFile4 = open("C:\\Temp\\Extra.txt", "a+")
        dataFile5 = open("C:\\Temp\\SUDelay.txt", "a+")
        dataFile6 = open("C:\\Temp\\PacketLoss.txt", "a+")
        dataFile7 = open("C:\\Temp\\MsgCount.txt", "a+")
        dataFile8 = open("C:\\Temp\\MCIRI.txt", "a+")

        firstTime = False
    
    ###################
    mjtime = time.time()
    mjpeers = ds.get_peerlist()
    #print >>sys.stderr, "PREBUF\t", ds.get_vod_prebuffering_progress()

    # START        
    if(x.data["CFLAG"][0] and x.data["BFLAG"][0] and x.data["LFLAG"][0] or (time.time() - x.data["STARTTIME"][0]) > timeout):
        x.delete("PEERS")

        for mjpeer in mjpeers:
            if(not x.is_existing("SU-"+mjpeer['ip'])):
                x.update("SU-"+mjpeer['ip'], time.time())
                MojoCommunicationClient(MJ_LISTENPORT,'[checksu]', mjpeer['ip'])
            if(str(mjpeer['ip']) not in x.data["PEERS"]):
                x.log("PEERS", str(mjpeer['ip']))

        x.update("STARTTIME", time.time())

    graceInt += 1
    if(len(x.data["PEERS"]) > 0 and graceInt >= 5):
        if(x.data["KFLAG"][0] and x.data["MFLAG"][0]):
            x.update("KLEN", len(x.data["PEERS"]))
            x.update("MLEN", len(x.data["PEERS"]))
            if(x.is_existing("HELPERS")):
                x.update("KLEN", float(x.data["KLEN"][0]) - len(x.data["HELPERS"]))
                x.update("MLEN", float(x.data["MLEN"][0]) - len(x.data["HELPERS"]))
            x.update("KFLAG", False)
            x.update("MFLAG", False)
            for peerip in x.data["PEERS"]:
                if(x.is_existing("HELPERS")):
                    if(str(peerip) not in x.data["HELPERS"]):
                        MojoCommunicationClient(MJ_LISTENPORT,'[GET-PCKT]', peerip)
                        MojoCommunicationClient(MJ_LISTENPORT,'[GET-NUMMSG]', peerip)
                else:
                    MojoCommunicationClient(MJ_LISTENPORT,'[GET-PCKT]', peerip)
                    MojoCommunicationClient(MJ_LISTENPORT,'[GET-NUMMSG]', peerip)

        if(x.data["CFLAG"][0] and x.data["BFLAG"][0]):
            bitRate = 200
            #toParse = ds.get_videoinfo()
            #bitRate = float(toParse['bitrate']*8/1024)
            #bitRate = toParse['bitrate']
            x.update("SUL", ds.get_current_speed(UPLOAD))
            x.update("SDL", ds.get_current_speed(DOWNLOAD))
            #x.update("BRATE", bitRate)
            x.update("CLEN", len(x.data["PEERS"]))
            x.update("CCHECK", 0)
            x.update("CFLAG", False)
            x.update("BFLAG", False)
            for peerip in x.data["PEERS"]:
                if(x.is_existing("HELPERS") and str(peerip) in x.data["HELPERS"]):
                    MojoCommunicationClient(MJ_LISTENPORT,'[uldl][1', peerip)
                else:
                    MojoCommunicationClient(MJ_LISTENPORT,'[uldl][0', peerip)

            if(x.data["LFLAG"][0] and (time.time() - x.data["LATTIME"][0]) >= latInt):
                get_latency()
                x.update("LFLAG", False)
                x.update("LATTIME", time.time())

            x.update("STARTTIME", time.time())

        print >>sys.stderr, "[PEERS]\t%s" % (x.data["PEERS"])
        print >>sys.stderr, "TIME\t", (time.time() - float(x.data["TIME"][0]))
        if((time.time() - float(x.data["TIME"][0]) >= twin and x.data["PFLAG"][0]) or time.time() - float(x.data["ACTIME"][0]) >= timeout):
            x.update("PLEN", len(x.data["PEERS"]))
            x.update("PCHECK", 0)
            x.update("PFLAG", False)
            x.update("TIME", time.time())
            x.update("ACTIME", time.time())
            #print >>sys.stderr, "[PEERS]\t", x.data["PEERS"]
            for peerip in x.data["PEERS"]:
                if(x.is_existing("HELPERS") and str(peerip) in x.data["HELPERS"]):
                    MojoCommunicationClient(MJ_LISTENPORT,'[aac][1', peerip)
                else:
                    MojoCommunicationClient(MJ_LISTENPORT,'[aac][0', peerip)

            x.update("STARTTIME", time.time())

    ####################

    dataFile.flush()
    dataFile2.flush()
    dataFile3.flush()
    dataFile4.flush()
    dataFile5.flush()
    dataFile6.flush()
    dataFile7.flush()
    dataFile8.flush()

    return (1.0,False)

def get_latency():
    if(len(x.data["PEERS"]) > 0 and float(x.data["LATCOUNT"][0]) == 0):
        x.update("LATCOUNT", len(x.data["PEERS"]))
        if(x.is_existing("HELPERS") and len(x.data["HELPERS"]) > 0):
            x.update("LATCOUNT", x.data["LATCOUNT"][0] - len(x.data["HELPERS"]))
        for mjpeer in x.data["PEERS"]:
            if(x.is_existing("HELPERS")):
                if(str(mjpeer) not in x.data["HELPERS"]):
                    x.update("LATENCY-"+str(mjpeer), time.time())
                    mojoLatencyTest(mjpeer)
            else:
                x.update("LATENCY-"+str(mjpeer), time.time())
                mojoLatencyTest(mjpeer)
    """
    #PACKET LOSS
    completePieces = ds.get_pieces_complete()
    numTrue = completePieces.count(True)
    #if(len(completePieces) > 0):
    """

def get_bandutil():
    if(len(x.data["PEERS"]) > 0 and float(x.data["BUCOUNT"][0]) == 0):
        x.update("BUCOUNT", len(x.data["PEERS"]))
        if(x.is_existing("HELPERS") and len(x.data["HELPERS"]) > 0):
            x.update("BUCOUNT", x.data["BUCOUNT"][0] - len(x.data["HELPERS"]))
        x.update("BUCHECK", 0)
        for mjpeer in x.data["PEERS"]:  
            if(x.is_existing("HELPERS")):
                if(str(mjpeer) not in x.data["HELPERS"]):
                    x.update("BUUP", 0.0)
                    x.update("BUDOWN", 0.0)
                    mojoBUSend(mjpeer)
            else:
                x.update("BUUP", 0.0)
                x.update("BUDOWN", 0.0)
                mojoBUSend(mjpeer)

def mjcompute_ciri():
    global collabInt

    if(x.data["HELPED"][0] and x.is_existing("HELPERS") and not x.data["ONHELP"][0]):
        if(x.is_existing("MCIRI")):
            x.update("MCIRI", 0.0)

        x.update("NetUpCon", 0.0)

        #print >>sys.stderr, "HELPERS:", x.data["HELPERS"]
        peercount = len(x.data["PEERS"])
        for mjpeer in x.data["HELPERS"]:
            if(str(mjpeer) in x.data["PEERS"]):
                x.update("NetUpCon", (x.data["NetUpCon"][0] + x.data["UL-"+str(mjpeer)][0] - x.data["DL-"+str(mjpeer)][0]))                    
            else:
                x.update("NetUpCon", (x.data["NetUpCon"][0] + 0.0))

        totalUpload = float(x.data["SUL"][0])
        peercount = peercount - len(x.data["HELPERS"])
        #print >>sys.stderr, "SUPLOAD\t", x.data["SUL"][0]

        if(peercount > 0):
            for mjpeer in x.data["PEERS"]:
                if(str(mjpeer) not in x.data["HELPERS"]):
                    totalUpload = totalUpload + float(x.data["UL-"+str(mjpeer)][0])
                    #print >>sys.stderr, "UPLOAD\t", x.data["UL-"+str(mjpeer)][0]

            x.update("MCIRI", (totalUpload + x.data["NetUpCon"][0])/(peercount*float(x.data["BRATE"][0])))
            print >>dataFile3,"%f\t%f" % (time.time(), x.data["MCIRI"][0])
            collabInt += 1
        
        #print >>sys.stderr, "totalUpload\t", totalUpload
        #print >>sys.stderr, "after\t", totalUpload + x.data["NetUpCon"][0]
        #print >>sys.stderr, "PEERCOUNT\t", peercount
        print >>dataFile8,"%f\t%f\t%f" % (time.time(), totalUpload, x.data["NetUpCon"][0])    
        
        if(x.is_existing("MCIRI") and x.data["MCIRI"][0] < 1 and collabInt == 15):
            mjmin_needed()
            MojoCommunicationClient(MJ_LISTENPORT,'[RENEW-MIN]XxX+XxX' + pickle.dumps(x.data["MIN-NEEDED"][0]), helpingSwarmIP)            
            collabInt = 0
    else:
        if(x.is_existing("CIRI")):
            x.delete("CIRI")

        totalUpload = float(x.data["SUL"][0])
        peercount = len(x.data["PEERS"])
        #print >>sys.stderr, "SUPLOAD\t", x.data["SUL"][0]

        if(peercount > 0):
            for mjpeer in x.data["PEERS"]:
                totalUpload = totalUpload + float(x.data["UL-"+str(mjpeer)][0])
                #print >>sys.stderr, "UPLOAD\t", x.data["UL-"+str(mjpeer)][0]

            x.update("CIRI", totalUpload/(peercount*float(x.data["BRATE"][0])))
            #print >>sys.stderr, "totalUpload\t", totalUpload            
            print >>dataFile3,"%f\t%f" % (time.time(), x.data["CIRI"][0])    

def mjcompute_rankings():
    if(x.is_existing("PEERS") and len(x.data["PEERS"]) > 0):
        if(checktime):
            AvgUL = 0.0
            AvgDL = 0.0
            for mjpeer in x.data["PEERS"]:
                AvgUL += x.data["ACUL-"+str(mjpeer)][0]
                AvgDL += x.data["ACDL-"+str(mjpeer)][0]

            x.update("AvgUL", AvgUL/float(len(x.data["PEERS"][0])))
            x.update("AvgDL", AvgDL/float(len(x.data["PEERS"][0])))
            #print >>sys.stderr,"[AvgULDL]\t%s\t%s" % (x.data["AvgUL"][0], x.data["AvgDL"][0])

        if(x.is_existing("AC-RANKED")):
            x.delete("AC-RANKED")
        if(x.is_existing("HIGH-RANKED")):
            x.delete("HIGH-RANKED")   
        if(x.is_existing("LOW-RANKED")):
            x.delete("LOW-RANKED")   

        #RANK PEERS ACCORDING TO AC IS POSSIBLE
        achash = {}
        for mjpeer in x.data["PEERS"]:
            if(x.is_existing("ACUL-"+str(mjpeer))):
                achash[float(x.data["ACUL-"+str(mjpeer)][0])] = str(mjpeer)
        ranked = sorted(achash, reverse=True)

        peerrank = []
        for mjpeerup in ranked:
            peerrank.append(str(achash[mjpeerup]))

        #print >>sys.stderr, "PEERRANK", peerrank

        for mjpeer in peerrank:
            x.log("AC-RANKED", mjpeer)

        print >>sys.stderr, "[AC-RANKED]\t%s" % (x.data["AC-RANKED"])

        for index in range(0, int(round(len(x.data["AC-RANKED"])/5 + .5))):
            x.log("HIGH-RANKED", x.data["AC-RANKED"][index])
        
        for index in range(0, int(round(len(x.data["AC-RANKED"])/5 + .5))):
            x.log("LOW-RANKED", x.data["AC-RANKED"][len(x.data["AC-RANKED"])-1 - index])
            
        global counter
        counter = counter + 1
        print >>sys.stderr,"help counter", counter
        #if(x.data["CIRI"][0] < 1):
        if counter == 20 and not x.data["ONHELP"][0]:
            if(x.is_existing("highpeers")):
                x.delete("highpeers")   
            if(x.is_existing("lowpeers")):
                x.delete("lowpeers")

            if(x.is_existing("HIGH-RANKED") and len(x.data["HIGH-RANKED"]) > 0):
                for index in range(0, int(len(x.data["HIGH-RANKED"]))):
                    hightemp = str(x.data["HIGH-RANKED"][index])
                    x.log("highpeers", hightemp)

            if(x.is_existing("LOW-RANKED") and len(x.data["LOW-RANKED"]) > 0):
                for index in range(0, int(len(x.data["LOW-RANKED"]))):
                    lowtemp = str(x.data["LOW-RANKED"][index])
                    x.log("lowpeers", lowtemp)

            #print >>sys.stderr,"SWARM NEEDS HELP"
            #print >>sys.stderr,"[HIGHEST AC]\t%s" % (x.data["highpeers"])
            #print >>sys.stderr,"[LOWEST AC]\t%s" % (x.data["lowpeers"])

            print >>dataFile4,"[HIGHPEERS]\t%s" % (x.data["highpeers"])
            print >>dataFile4,"[LOWPEERS]\t%s" % (x.data["lowpeers"])
            
            counter = 0
            if not x.data["HELPED"][0]:
                #print >>sys.stderr,"Calling the getHelp() function..."
                mjmin_needed()
                #getHelp(x.data["highpeers"], x.data["lowpeers"], x.data["MIN-NEEDED"][0])

def mjmin_needed():
    if(x.is_existing("MIN-NEEDED")):
        x.delete("MIN-NEEDED")

    bitRate = float(x.data["BRATE"][0])
    peercount = len(x.data["PEERS"])
    if(x.is_existing("HELPERS")):
        peercount = peercount - len(x.data["HELPERS"])
    minBandwidth = peercount*bitRate
    totalUpload = float(x.data["SUL"][0])
    if(peercount > 0):
        for mjpeer in x.data["PEERS"]:
            if(x.is_existing("HELPERS")):
                if(str(mjpeer) not in x.data["HELPERS"]):
                    totalUpload = totalUpload + float(x.data["UL-"+str(mjpeer)][0])
            else:
                totalUpload = totalUpload + float(x.data["UL-"+str(mjpeer)][0])

    minBandwidth = minBandwidth - totalUpload

    x.log("MIN-NEEDED", minBandwidth)
    print >>dataFile4, "[MIN-NEEDED]\t", x.data["MIN-NEEDED"][0]

def mjbandwidth_allocation(mjpeer, minNeeded, numPeers):
    minPerPeer = minNeeded/numPeers
    AlphaUL = 2
    AlphaDL = 2

    if(float(x.data["ACDL-"+str(mjpeer)][0]) > x.data["AvgDL"][0]):
        excessDL = float(x.data["ACDL-"+str(mjpeer)][0]) - x.data["AvgDL"][0] 
    else:
        excessDL = x.data["AvgDL"][0] - float(x.data["ACDL-"+str(mjpeer)][0])
    """
    usedDL = float(x.data["ACDL-"+str(mjpeer)][0])
    BetaDL = excessDL/usedDL
    leftSide = BetaDL*(excessDL)
    rightSide = (1 - BetaDL)*(usedDL)
    preTotalDL = leftSide + rightSide
    """
    preTotalDL = excessDL

    excessUL = float(x.data["ACUL-"+str(mjpeer)][0]) - x.data["AvgUL"][0] 
    usedUL = float(x.data["ACUL-"+str(mjpeer)][0])
    BetaUL = excessUL/usedUL
    leftSide = BetaUL*(excessUL)
    rightSide = (1 - BetaUL)*(usedUL)
    preTotalUL = leftSide + rightSide

    totalDL = preTotalDL
    totalUL = preTotalUL

    while(totalDL > totalUL and AlphaDL > 1):
        AlphaDL = AlphaDL - .2
        totalDL = preTotalDL * AlphaDL

    preNetUp = totalUL - preTotalDL
    while(preNetUp < minPerPeer and AlphaUL <= 3):
        AlphaUL = AlphaUL + .2
        totalUL = preTotalUL * AlphaUL
        preNetUp = totalUL - totalDL
    """
    if(preNetUp < minPerPeer):
        AlphaUL = (minPerPeer + preTotalDL) / preTotalUL

    s.t. preTotalUL*AlphaUL - preTotalDL*AlphaDL >= minPerPeer
    """

    x.update("BAUL-"+str(mjpeer), totalUL)
    x.update("BADL-"+str(mjpeer), totalDL)
    #print >>sys.stderr, "[MJ-AVGUP]\t%f" % (float(x.data["AvgUp"][0]))
    #print >>sys.stderr, "[MJ-AAC-%s]\t%f" % (mjpeer, float(x.data["AAC-"+str(mjpeer)][0]))
    print >>sys.stderr, "[BAUL-%s]\t%f" % (mjpeer, float(x.data["BAUL-"+str(mjpeer)][0]))
    print >>sys.stderr, "[BADL-%s]\t%f" % (mjpeer, float(x.data["BADL-"+str(mjpeer)][0]))
    print >>dataFile4, "[BAUL-%s]\t%f" % (mjpeer, float(x.data["BAUL-"+str(mjpeer)][0]))
    print >>dataFile4, "[BADL-%s]\t%f" % (mjpeer, float(x.data["BADL-"+str(mjpeer)][0]))

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
    #print >>sys.stderr,"[MJ-Notif-Host] Callback function in main received: ", msg    
    global checktime

    if msg.startswith('[HELP]'):
        temp = msg.split("XxX+XxX")
        #print >>sys.stderr, "CHECKING", temp[0]
        helpedTorrentDef = pickle.loads(temp[1])
        helpedhighpeers = pickle.loads(temp[2])
        helpedlowpeers = pickle.loads(temp[3])
        minNeeded = pickle.loads(temp[4])
        # Get the peers with lowest absCon
        
        # For each helping peers, call the function sendMojoTstream with their IP address as arguments
        # sendMojoTstream(ipAddr)
        if x.is_existing("highpeers"):
            for mjpeer in  x.data["highpeers"]:
                mjbandwidth_allocation(mjpeer, minNeeded, len(x.data["highpeers"]))
                sendMojoTstream(mjpeer, helpedTorrentDef, helpedhighpeers + [addr[0]], helpedlowpeers, x.data["BAUL-"+str(mjpeer)][0], x.data["BADL-"+str(mjpeer)][0])
        
        # Reply to the helped swarm with your peer list
        x.update("ONHELP", True)

        MojoCommunicationClient(MJ_LISTENPORT,'[ACK-HELP]XxX+XxX' + pickle.dumps(x.data["highpeers"]) + 'XxX+XxX' + pickle.dumps(x.data["lowpeers"]), addr[0])

    elif msg.startswith('[latencyrep]'):
        x.update("LATENCY-"+str(addr[0]), time.time() - float(x.data["LATENCY-"+str(addr[0])][0]))
        x.log("AVGLATENCY", float(x.data["LATENCY-"+str(addr[0])][0]))
        #print >>sys.stderr,"[Lat-%s]\t%s" % (addr[0], x.data["LATENCY-"+str(addr[0])][0])
        #print >>sys.stderr,"[AvgLat]\t%s" % (x.data["AVGLATENCY"][0])

        if(float(len(x.data["AVGLATENCY"])) == float(x.data["LATCOUNT"][0]) and x.data["LATCOUNT"][0] > 0):
            print >>dataFile, "%f\t%f" % (time.time(), x.averageData("AVGLATENCY"))
            x.update("GUILATENCY", x.averageData("AVGLATENCY"))
            x.delete("AVGLATENCY")
            x.update("LATCOUNT", 0)
            x.update("LFLAG", True)

    elif msg.startswith('[maxspeed]'):
        temp = msg.split("][")
        addBUUP = pickle.loads(temp[1])
        addBUDOWN = pickle.loads(temp[2])
        x.update("BUUP", float(x.data["BUUP"][0]) + addBUUP)
        x.update("BUDOWN", float(x.data["BUDOWN"][0]) + addBUDOWN)
        x.update("BUCHECK", float(x.data["BUCHECK"][0]) + 1)

        if(float(x.data["BUCHECK"][0]) == float(x.data["BUCOUNT"][0])):
            totalUpload = x.data["SUL"][0]
            totalDownload = x.data["SDL"][0]
            #print >>sys.stderr, "[SULDL]\t%s\t%s" % (totalUpload, totalDownload)
            for mjpeer in x.data["PEERS"]:
                if(x.is_existing("HELPERS")):
                    if(str(mjpeer) not in x.data["HELPERS"]):
                        totalUpload += x.data["UL-"+str(mjpeer)][0]
                        totalDownload += x.data["DL-"+str(mjpeer)][0]    
                else:
                    totalUpload += x.data["UL-"+str(mjpeer)][0]
                    totalDownload += x.data["DL-"+str(mjpeer)][0]    
            #print >>sys.stderr, "[TULDL]\t%s\t%s" % (totalUpload, totalDownload)
            #print >>sys.stderr, "[BULDL]\t%s\t%s" % (x.data["BUUP"][0], x.data["BUDOWN"][0])
            buUp = totalUpload / float(x.data["BUUP"][0])
            buDown = totalDownload / float(x.data["BUDOWN"][0])
            x.update("BUUP", buUp)
            x.update("BUDOWN", buDown)
            print >>dataFile2, "%f\t%f\t%f" % (time.time(), buUp, buDown)
            #print >>dataFile2, "%f\t%f" % (time.time(), buUp)
            x.update("BUCOUNT", 0)
            x.update("BUCHECK", 0)
            x.update("BFLAG", True)

    elif msg.startswith('[sudelay]'):
        #print >>sys.stderr, "HOY\t", msg
        x.update("SU-"+str(addr[0]), time.time() - x.data["SU-"+str(addr[0])][0])
        print >>dataFile5, "[SU-%s]\t%s" % (addr[0], x.data["SU-"+str(addr[0])][0])
        dGlobal.set_flag(addr[0])
        
    elif msg.startswith('[ACK-HELP]'):
        temp = msg.split("XxX+XxX")
        helpingPeers = pickle.loads(temp[1])
        x.equate("HELPERS", helpingPeers)
        x.update("OLDPC", len(x.data["PEERS"]))

    ####################
    elif msg.startswith('[uldl]'):
        temp = msg.split("][")
        peerul = pickle.loads(temp[1])
        peerdl = pickle.loads(temp[2])
        x.update("UL-"+str(addr[0]), peerul)
        x.update("DL-"+str(addr[0]), peerdl)
        x.update("CCHECK", float(x.data["CCHECK"][0]) + 1)
        #print >>sys.stderr, "[ULDL-%s]\t%s\t%s" % (addr[0], x.data["UL-"+str(addr[0])][0], x.data["DL-"+str(addr[0])][0])
        if(x.data["CCHECK"][0] == x.data["CLEN"][0]):
            mjcompute_ciri()
            get_bandutil()
            x.update("CLEN", 0)
            x.update("CCHECK", 0)
            x.update("CFLAG", True)

    elif msg.startswith('[aac]'):
        temp = msg.split("][")
        peerul = pickle.loads(temp[1])
        peerdl = pickle.loads(temp[2])
        x.update("ACUL-"+str(addr[0]), peerul)
        x.update("ACDL-"+str(addr[0]), peerdl)
        #print >>sys.stderr, "[CHECKCHECK1]\t", x.data["PCHECK"][0]
        x.update("PCHECK", float(x.data["PCHECK"][0]) + 1)
        #print >>sys.stderr, "[ACULDL-%s]\t%s\t%s" % (addr[0], x.data["ACUL-"+str(addr[0])][0], x.data["ACDL-"+str(addr[0])][0])
        #print >>sys.stderr, "[CHECKCHECK2]\t", x.data["PCHECK"][0]
        if(x.data["PCHECK"][0] >= x.data["PLEN"][0]):
            checktime = True
            mjcompute_rankings()
            x.update("PLEN", 0)
            x.update("PCHECK", 0)
            x.update("PFLAG", True)

    elif msg.startswith('[RENEW-MIN]'):
        temp = msg.split("XxX+XxX")
        minNeeded = pickle.loads(temp[1])
        if x.is_existing("highpeers"):
            for mjpeer in  x.data["highpeers"]:
                mjbandwidth_allocation(mjpeer, minNeeded, len(x.data["highpeers"]))
                MojoCommunicationClient(MJ_LISTENPORT,'[REALLOC]XxX+XxX' + pickle.dumps(x.data["BAUL-"+str(mjpeer)][0]) + 'XxX+XxX' + pickle.dumps(x.data["BADL-"+str(mjpeer)][0]), mjpeer)

    elif msg.startswith('[PACKET]'):
        temp = msg.split("][")
        pcktLoss = pickle.loads(temp[1])
        x.log("PACKETLOSS", pcktLoss)
        if(len(x.data["PACKETLOSS"]) == x.data["KLEN"][0] and x.data["KLEN"][0] > 0):
            print >>dataFile6,"%f\t%f" % (time.time(), x.averageData("PACKETLOSS"))
            x.update("GUIPACKET", x.averageData("PACKETLOSS"))
            x.delete("PACKETLOSS")
            x.update("KLEN", 0)
            x.update("KFLAG", True)

    elif msg.startswith('[NUMMSG]'):
        temp = msg.split("][")
        numMsgs = pickle.loads(temp[1])
        x.log("MSGCOUNT", numMsgs)
        if(len(x.data["MSGCOUNT"]) == x.data["MLEN"][0] and x.data["MLEN"][0] > 0):
            print >>dataFile7,"%f\t%f" % (time.time(), x.averageData("MSGCOUNT"))
            x.update("GUIMSG", x.averageData("MSGCOUNT"))
            x.delete("MSGCOUNT")
            x.update("MLEN", 0)
            x.update("MFLAG", True)

def getHelp(highpeers, lowpeers, minNeeded):   
    global helpingSwarmIP 
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
    
    #helpingSwarmIP = "192.168.1.40" #get from tracker
    helpingSwarmIP = "192.168.0.69" #get from tracker
    # After some time
    print >>sys.stderr,"Helping swarm found. Initiating connection." 
    x.update("HELPED",True);
    print >>dataFile,"@===============================[SWARM HELPED]===============================" 
    print >>dataFile2,"@===============================[SWARM HELPED]===============================" 
    print >>dataFile3,"@===============================[SWARM HELPED]===============================" 
    print >>dataFile4,"@===============================[SWARM HELPED]===============================" 
    print >>dataFile5,"@===============================[SWARM HELPED]===============================" 
    print >>dataFile6,"@===============================[SWARM HELPED]===============================" 
    print >>dataFile7,"@===============================[SWARM HELPED]===============================" 
    print >>dataFile8,"@===============================[SWARM HELPED]===============================" 
    #print >>sys.stderr,"orig tdef " + pickle.dumps(origTdef)
    MojoCommunicationClient(MJ_LISTENPORT,'[HELP]XxX+XxX' + pickle.dumps(origTdef) + 'XxX+XxX' + pickle.dumps(highpeers) + 'XxX+XxX' + pickle.dumps(lowpeers) + 'XxX+XxX' + pickle.dumps(minNeeded), helpingSwarmIP)
    
def sendMojoTstream(ipAddr, torrentdef, highpeers, lowpeers, bandwidthAllocUL, bandwidthAllocDL):
    """ Called by MojoCommunication thread """
    print >>sys.stderr,"Sending tstream... ", ipAddr
    #createTorrentDef()
    MojoCommunicationClient(MJ_LISTENPORT,'[download-tstream]XxX+XxX' + pickle.dumps(torrentdef) + 'XxX+XxX' + pickle.dumps(highpeers) + 'XxX+XxX' + pickle.dumps(lowpeers) + 'XxX+XxX' + pickle.dumps(bandwidthAllocUL) + 'XxX+XxX' + pickle.dumps(bandwidthAllocDL), ipAddr)

def mojoBUSend(ipAddr):
    # do what you want to do to the recieved message in the main thread. hekhek
    MojoCommunicationClient(MJ_LISTENPORT,'[maxspeed]', ipAddr)

def mojoLatencyTest(ipAddr):
    # do what you want to do to the recieved message in the main thread. hekhek
    MojoCommunicationClient(MJ_LISTENPORT,'[latencytest]', ipAddr)
    
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
'''
class PlayerApp(wx.App):
    def __init__(self, x):
        wx.App.__init__(self, x)
        
    def OnInit(self):
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
'''        
if __name__ == "__main__":
    global origTdef
    global dGlobal
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

    #app = PlayerApp(0)
    #app.MainLoop()
    
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

    dscfg.set_max_speed(UPLOAD, MOJOMAXUPLOAD)
    
    # limit the # of connections to the server to only ONE peer so that other peers will connect to each other and not to server only
    # change this later so that number of connected peers  = totalServerUpload/bitrate
    # MOJO - tested and working hekhek
    # dscfg.set_max_conns_to_initiate(1)
    #print >>sys.stderr,"MAX CONNECTIONS:\t",dscfg.get_max_conns()
    #dscfg.set_max_conns(2)

    d = s.start_download(tdef,dscfg)
    d.set_state_callback(state_callback,getpeerlist=True)
    dGlobal = d

    # Start UI
    app = wx.App(redirect=False)
    top = ServerFrame("Tribler Server Monitor")
    top.Show()
    app.MainLoop()
   
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
