# Written by MoJo based on Instance2Instance.py
# see LICENSE.txt for license information

import sys
import socket
from StringIO import StringIO
from traceback import print_exc
from threading import Thread

from Tribler.Core.BitTornado.BT1.convert import tobinary,toint

def readn(s,n,buffer):
    """ read n bytes from socket stream s, using buffer as aid """
    nwant = n
    while True:
        try:
            data = s.recv(nwant)
        except socket.error, e:
            if e[0] == 10035: # WSAEWOULDBLOCK on Windows
                continue
            else:
                raise e
        if len(data) == 0:
            return data
        nwant -= len(data)
        buffer.write(data)
        if nwant == 0:
            break
    buffer.seek(0)
    data = buffer.read(n)
    buffer.seek(0)
    return data


class MojoCommunicationServer(Thread):
    
    def __init__(self,port,callback):
        Thread.__init__(self)
        self.setDaemon(True)
        self.setName('MojoCommunication'+self.getName())
        self.port = port
        self.callback = callback
        
        self.ss = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.ss.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        # print "IP addr: ", socket.gethostbyname(socket.gethostname())
        # print "socket name: ", self.ss.getsockname()
        # TODO: Check for timeout in establishing connections
        # Check if it can listen to more than 1 client
        self.ss.bind(('0.0.0.0', self.port))
        self.ss.listen(5)
        
    def run(self):
        print "MojoCommunication listening..."
        while True:
            try:
                conn, addr = self.ss.accept()
                print >>sys.stderr, "IP Address: ", addr
                buffer = StringIO()
                sizedata = readn(conn,4,buffer)
                size = toint(sizedata)
                msg = readn(conn,size,buffer)
                #print >>sys.stderr, "[MJ-Notif] Received Message: ", msg
                
                self.callback(addr, msg)
                conn.close()
                
            except:
                print "Unexpected error:", sys.exc_info()[0]
                print_exc()
        

class MojoCommunicationClient:
    
    def __init__(self,port,msg,ipaddr='127.0.0.1'):
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.connect((ipaddr,port))
        sizedata = tobinary(len(msg))
        s.send(sizedata)
        s.send(msg)
        s.close()
