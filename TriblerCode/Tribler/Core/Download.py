# Written by Arno Bakker 
# see LICENSE.txt for license information
""" The representation of a running BT download/upload """

import sys
import os
#import time
import copy
import sha
import pickle
import shutil
from traceback import print_exc,print_stack
from types import StringType,ListType,IntType

from Tribler.Core.simpledefs import *
from Tribler.Core.defaults import *
from Tribler.Core.exceptions import *
from Tribler.Core.Base import *
from Tribler.Core.APIImplementation.DownloadRuntimeConfig import DownloadRuntimeConfig
from Tribler.Core.APIImplementation.DownloadImpl import DownloadImpl
from Tribler.Core.APIImplementation.miscutils import *

from Tribler.Core.Utilities.unicode import metainfoname2unicode
from Tribler.Core.osutils import *


class Download(DownloadRuntimeConfig,DownloadImpl):
    """
    Representation of a running BT download/upload
    
    A Download implements the DownloadConfigInterface which can be used to
    change download parameters are runtime (for selected parameters).
    
    cf. libtorrent torrent_handle
    """
    
    #
    # Internal methods
    #
    def __init__(self,session,tdef):
        """ Internal constructor
        @param session Session
        @param tdef TorrentDef 
        """
        DownloadImpl.__init__(self,session,tdef)
    #
    # Public methods
    #
    def get_def(self):
        """
        Return the read-only torrent definition (TorrentDef) for this Download.
        @return A TorrentDef object.
        """
        return DownloadImpl.get_def(self)

    
    def set_state_callback(self,usercallback,getpeerlist=True):
        """ 
        Set a callback for retrieving the state of the download. This callback
        will be called immediately with a DownloadState object as first parameter.
        The callback method must return a tuple (when,getpeerlist) where "when" 
        indicates whether the callback should be called again and represents a
        number of seconds from now. If "when" <= 0.0 the callback will not be
        called again. "getpeerlist" is a boolean that indicates whether the 
        DownloadState passed to the callback on the next invocation should
        contain info about the set of current peers.
        
        The callback will be called by a popup thread which can be used
        indefinitely (within reason) by the higher level code.
                
        @param usercallback Function that accepts DownloadState as parameter and 
        returns a (float,boolean) tuple.
        """
        DownloadImpl.set_state_callback(self,usercallback,getpeerlist=getpeerlist)
        

    def stop(self):
        """ Stops the Download, i.e. closes all connections to other peers. """
        # Called by any thread 
        DownloadImpl.stop(self)
        
    def restart(self):
        """ Restarts the stopped Download. """
        # Called by any thread
        DownloadImpl.restart(self)
        
    def update_peerlist(self, highpeers, lowpeers):
        """ Updates peerlist. """
        # Called by any thread
        return DownloadImpl.update_peerlist(self, highpeers, lowpeers)

    def get_packet_loss(self):
        # Called by any thread
        return DownloadImpl.get_packet_loss(self)

    def get_num_msgs(self):
        # Called by any thread
        return DownloadImpl.get_num_msgs(self)
       
    def set_server_ip(self, ipAddr):
        # Called by any thread
        DownloadImpl.set_server_ip(self, ipAddr)

    def get_server_ip(self):
        # Called by any thread
        return DownloadImpl.get_server_ip(self)

    def set_flag(self, ipAddr):
        return DownloadImpl.set_flag(self, ipAddr)
    #
    # Config parameters that only exists at runtime 
    #
    def set_max_desired_speed(self,direct,speed):
        """ Sets the maximum desired upload/download speed for this Download. 
        @param direct The direction (UPLOAD/DOWNLOAD) 
        @param speed The speed in KB/s.
        """
        DownloadImpl.set_max_desired_speed(self,direct,speed)

    def get_max_desired_speed(self,direct):
        """ Returns the maximum desired upload/download speed for this Download.
        @return The previously set speed in KB/s 
        """
        return DownloadImpl.get_max_desired_speed(self,direct)
    
    def get_dest_files(self, exts = None):
        """ Returns the filenames on disk to which this Download saves
        @return A list of (filename-in-torrent, disk filename) tuples.
        """
        return DownloadImpl.get_dest_files(self, exts)
        
    #
    # Cooperative download
    #
    def ask_coopdl_helpers(self,permidlist):
        """ Ask the specified list of peers to help speed up this download """
        # called by any thread
        self.dllock.acquire()
        try:
            # ARNOCOMMENT: WE NEED PERMID+IP FOR COOP DL. How to access DB? Can't
            # do it on main thread, can't do it on network thread.
            
            peerreclist = self.session.lm.peer_db.getPeers(permidlist, ['permid','ip','port'])
            
            if self.sd is not None:
                ask_coopdl_helpers_lambda = lambda:self.sd.ask_coopdl_helpers(peerreclist)
                self.session.lm.rawserver.add_task(ask_coopdl_helpers_lambda,0)
            else:
                raise OperationNotPossibleWhenStoppedException()
        finally:
            self.dllock.release()
        
    # To retrieve the list of current helpers, see DownloadState
    
    def stop_coopdl_helpers(self,permidlist):
        """ Ask the specified list of peers to stop helping speed up this 
        download """
        # called by any thread
        self.dllock.acquire()
        try:
            # ARNOCOMMENT: WE NEED PERMID+IP FOR COOP DL. How to access DB? Can't
            # do it on main thread, can't do it on network thread.
            peerreclist = self.session.lm.peer_db.getPeers(permidlist, ['permid','ip','port'])
                       
            if self.sd is not None:
                stop_coopdl_helpers_lambda = lambda:self.sd.stop_coopdl_helpers(peerreclist)
                self.session.lm.rawserver.add_task(stop_coopdl_helpers_lambda,0)
            else:
                raise OperationNotPossibleWhenStoppedException()
        finally:
            self.dllock.release()
