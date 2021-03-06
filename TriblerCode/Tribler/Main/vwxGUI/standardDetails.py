import wx.xrc as xrc
from binascii import hexlify
from time import sleep,time
import math
from traceback import print_exc, print_stack
import cStringIO
import urlparse
from wx.lib.stattext import GenStaticText as StaticText

import threading

from font import *
from Tribler.Main.vwxGUI.GuiUtility import GUIUtility
from Tribler.Main.vwxGUI.IconsManager import IconsManager, data2wxBitmap
from Tribler.Main.vwxGUI.filesItemPanel import loadAzureusMetadataFromTorrent,createThumbImage
from Tribler.Main.Dialogs.GUITaskQueue import GUITaskQueue
from Tribler.Core.Overlay.OverlayThreadingBridge import OverlayThreadingBridge
from Tribler.Core.Overlay.MetadataHandler import get_filename
from Tribler.Main.Utility.constants import COL_PROGRESS
from Tribler.TrackerChecking.TorrentChecking import TorrentChecking
from Tribler.Video.VideoPlayer import VideoPlayer
from Tribler.Main.vwxGUI.tribler_List import DLFilesList

from Tribler.Core.API import *
from Tribler.Core.Utilities.utilities import *
from Tribler.Core.Utilities.unicode import bin2unicode

# LAYERVIOLATION
from Tribler.Core.CacheDB.CacheDBHandler import GUIDBHandler, BarterCastDBHandler
from Tribler.Core.CacheDB.EditDist import editDist


DETAILS_MODES = ['filesMode', 'personsMode', 'profileMode', 'libraryMode', 'friendsMode', 'subscriptionsMode', 'messageMode']

DEBUG = False

def showInfoHash(infohash):
    if infohash.startswith('torrent'):    # for testing
        return infohash
    try:
        n = int(infohash)
        return str(n)
    except:
        pass
    return encodestring(infohash).replace("\n","")
            
class standardDetails(wx.Panel):
    """
    Wrappers around details xrc panels
    """
    def __init__(self, *args):
        if len(args) == 0:
            pre = wx.PrePanel()
            # the Create step is done by XRC.
            self.PostCreate(pre)
            self.Bind(wx.EVT_WINDOW_CREATE, self.OnCreate)
        else:
            wx.Panel.__init__(self, *args)
            self._PostInit()
        
    def OnCreate(self, event):
        self.Unbind(wx.EVT_WINDOW_CREATE)
        wx.CallAfter(self._PostInit)
        event.Skip()
        return True
    
    def _PostInit(self):
        # Do all init here
        self.subscr_old_source = None
        self.guiUtility = GUIUtility.getInstance()
        self.utility = self.guiUtility.utility        
        self.torrent_db = self.utility.session.open_dbhandler(NTFY_TORRENTS)
        self.friend_db = self.utility.session.open_dbhandler(NTFY_FRIENDS)
        self.peer_db = self.utility.session.open_dbhandler(NTFY_PEERS)
        self.superpeer_db = self.utility.session.open_dbhandler(NTFY_SUPERPEERS)
        #self.optionsButtonLibraryFunc = rightMouseButton.getInstance()
        self.iconsManager = IconsManager.getInstance()
        #self.gui_db = GUIDBHandler.getInstance()
                                    
        self.mode = None
        self.item = None
        self.bartercastdb = None
        self.lastItemSelected = {} #keeps the last item selected for each mode
        self.data = {} #keeps gui elements for each mode
        for mode in DETAILS_MODES+['status']:
            self.data[mode] = {} #each mode has a dictionary of gui elements with name and reference
            self.lastItemSelected[mode] = None
        self.currentPanel = None
        self.videoplayer = VideoPlayer.getInstance()
        self.addComponents()
        
        #self.Refresh()
        self.modeElements = {}
        for elem in DETAILS_MODES:
            self.modeElements[elem] = []
        self.modeElements['filesMode'] = ['titleField', 'simTitlesField', 'popularityField1', 'options', 'popularityField2', 'creationdateField', 
                                            'descriptionField', 'sizeField', 'thumbField', 'up', 'down', 'refresh', 
                                            'download', 'tabs', ('files_detailsTab','tabs'), ('info_detailsTab','tabs'), 
                                            'TasteHeart', 'details', 'peopleWhoField', 'recommendationField']
        self.modeElements['personsMode'] = ['TasteHeart', 'recommendationField','addAsFriend', 'commonFilesField', 'commonFiles',
                                            'alsoDownloadedField', 'alsoDownloaded', 'info_detailsTab', 'advanced_detailsTab','detailsC',
                                            'titleField','statusField','thumbField', 'discFilesField', 'discPersonsField']
        self.modeElements['friendsMode'] = ['TasteHeart', 'recommendationField','addAsFriend', 'commonFilesField', 'commonFiles',
                                            'alsoDownloadedField', 'alsoDownloaded', 'info_detailsTab', 'advanced_detailsTab','detailsC',
                                            'titleField','statusField','thumbField', 'discFilesField', 'discPersonsField']
        self.modeElements['libraryMode'] = ['titleField', 'simTitlesField', 'popularityField1','options', 'popularityField2', 'creationdateField', 
                                            'descriptionField', 'sizeField', 'thumbField', 'up', 'down', 'refresh', 
                                            'files_detailsTab', 'info_detailsTab', 'details', 
                                            'peopleWhoField']
        self.modeElements['profileMode'] = ['levelPic', 'uploadedNumber', 'downloadedNumber']
        
        
        self.modeElements['subscriptionsMode'] = ['titleField', 'receivedToday', 'subscrTodayField', 'receivedYesterday', 'subscrYesterdayField'] #  'receivedTotal']
        
        self.tabElements = {'filesTab_files': [ 'download', 'includedFiles', 'filesField', 'trackerField'],                            
                            'personsTab_advanced': ['lastExchangeField', 'timesConnectedField','addAsFriend','similarityValueField'],
                            'libraryTab_files': [ 'download', 'includedFiles'],
                            'profileDetails_Quality': ['descriptionField0','howToImprove','descriptionField1'],
                            'profileDetails_Files': ['descriptionField0','howToImprove','descriptionField1','takeMeThere0'],
                            'profileDetails_Persons': ['descriptionField0','howToImprove','descriptionField1'],
                            'profileDetails_Download': ['descriptionField','Desc0','descriptionField0','howToImprove0','descriptionField1','takeMeThere0','Desc1','descriptionField2','howToImprove1','descriptionField3','takeMeThere1','Desc2','descriptionField4','howToImprove2','descriptionField5','takeMeThere2'],
                            #'profileDetails_Presence': ['descriptionField','Desc0','descriptionField0','howToImprove0','descriptionField1','Desc1','descriptionField2','howToImprove1','descriptionField3','Desc2','descriptionField4','howToImprove2','descriptionField5','takeMeThere0']}
                            'profileDetails_Presence': ['descriptionField','Desc0','descriptionField0','howToImprove0','descriptionField1','Desc2','descriptionField4','howToImprove2','descriptionField5','takeMeThere0'],
                            'profileDetails_statsTopSharers':['descriptionField0', 'downloadedNumberT', 'uploadedNumberT']}
        
            
        self.statdlElements = ['st28c','down_White','downSpeed','up_White','upSpeed','download1','percent1','download2','percent2','download3','percent3','download4','percent4']
            
        self.guiUtility.initStandardDetails(self)
        

    def addComponents(self):
        self.SetBackgroundColour(wx.Colour(102,102,102))
        #self.SetBackgroundColour(wx.Colour(255,51,0))
        self.hSizer = wx.BoxSizer(wx.VERTICAL)
        self.SetSizer(self.hSizer)
        self.SetAutoLayout(1)
        self.Layout()
        #print "tb"
        #print self.GetSize()
    
        
    def setMode(self, mode, item = None):
        
        if DEBUG:
            print >>sys.stderr,"standardDetails: setMode called, new mode is",mode,"old",self.mode
        
        if self.mode != mode:
            #change the mode, so save last item selected
            self.lastItemSelected[self.mode] = self.item
            self.mode = mode
            self.checkGraphTabVisible()
            self.refreshMode()
        if item:
            self.setData(item)
        elif self.lastItemSelected[self.mode]:
            self.guiUtility.selectData(self.lastItemSelected[self.mode])
        else:
            self.setData(None)
    
    def getMode(self):
        return self.mode
            
    def refreshMode(self):
        # load xrc
        self.oldpanel = self.currentPanel
        #self.Show(False)
        
        self.currentPanel = self.loadPanel()
        assert self.currentPanel, "Panel could not be loaded"
        self.currentPanel.Layout()
        self.currentPanel.SetAutoLayout(1)
        #self.currentPanel.Enable(True)
#        self.currentPanel.SetBackgroundColour("red")
        
        self.currentPanel.Show(True)
        
        if self.oldpanel:
            self.hSizer.Detach(self.oldpanel)
            self.oldpanel.Hide()
            #self.oldpanel.Disable()
        
        self.hSizer.Insert(0, self.currentPanel, 0, wx.ALL|wx.EXPAND, 0)
        
            
#        self.currentPanel.Layout()
        wx.CallAfter(self.hSizer.Layout)
        wx.CallAfter(self.refreshStandardDetailsHeight)
#        wx.CallAfter(self.currentPanel.Refresh)
        #self.Show(True)
        

    def refreshStatusPanel(self, show):
        if show:
            statusPanel = self.data['status'].get('panel')
            if not statusPanel:
                statusPanel = self.loadStatusPanel()
                self.data['status']['panel'] = statusPanel
            #statusPanel.Enable()
            statusPanel.Show()
            self.hSizer.Insert(1, statusPanel, 0, wx.TOP|wx.EXPAND, 6)
            self.hSizer.Layout()
        else:
            # Remove statusPanel if necessary
            if self.data['status'].get('panel'):
                statusPanel = self.data['status']['panel']
                try:
                    self.hSizer.Detach(statusPanel)
                    statusPanel.Hide()
                    #statusPanel.Disable()
                except:
                    print_exc()
        
    def setListAspect2OneColumn(self, list_name):
        ofList = self.getGuiObj(list_name)
        ofList.ClearAll()
        if sys.platform == 'win32':
            ofList.SetWindowStyleFlag(wx.LC_REPORT|wx.NO_BORDER|wx.LC_NO_HEADER|wx.LC_SINGLE_SEL) #it doesn't work
        else:
            ofList.SetSingleStyle(wx.NO_BORDER)
            ofList.SetSingleStyle(wx.LC_REPORT)
            ofList.SetSingleStyle(wx.LC_NO_HEADER)
            ofList.SetSingleStyle(wx.LC_SINGLE_SEL)
        ofList.InsertColumn(0, "Torrent") #essential code
#        ofList.SetColumnWidth(0,wx.LIST_AUTOSIZE)
        
    def loadPanel(self):
        currentPanel = self.data[self.mode].get('panel',None)
        modeString = self.mode[:-4]
        #<mluc>[11.05.07]: small hack as the friends mode has no details panel, but we still want to know that this is friends mode
        if self.mode == "friendsMode":
            modeString = "persons"
        if not currentPanel:
            xrcResource = os.path.join(self.guiUtility.vwxGUI_path, modeString+'Details.xrc')
            panelName = modeString+'Details'
            currentPanel = self.loadXRCPanel(xrcResource, panelName)
            # Save paneldata in self.data
            self.data[self.mode]['panel'] = currentPanel
            #titlePanel = xrc.XRCCTRL(currentPanel, 'titlePanel')
            
            if self.modeElements.has_key(self.mode):
                for element in self.modeElements[self.mode]:
                    xrcElement = None
                    name = None
                    if type(element) == str:
                        xrcElement = xrc.XRCCTRL(currentPanel, element)
                        name = element
                    elif type(element) == tuple:
                        name = element[0]
                        xrcElement = xrc.XRCCTRL(self.getGuiObj(element[1]), name)
                    if not xrcElement:
                        if DEBUG:
                            print  >> sys.stderr,'standardDetails: Error: Could not identify xrc element: %s for mode %s' % (element, self.mode)
                        pass
                    if name:
                        self.data[self.mode][name] = xrcElement
            else:
                self.modeElements[self.mode] = []
            
            # do extra init
            if modeString in ['files','library']:
                self.getGuiObj('up').setBackground(wx.WHITE)
                self.getGuiObj('down').setBackground(wx.WHITE)
                refresh = self.getGuiObj('refresh')
                refresh.setBackground(wx.WHITE)
                refresh.Bind(wx.EVT_ENTER_WINDOW, self.updateLastCheck)
                self.setListAspect2OneColumn("peopleWhoField")
                self.setListAspect2OneColumn("simTitlesField")
                infoTab = self.getGuiObj('info_detailsTab')
                infoTab.setSelected(True)
                self.getAlternativeTabPanel('filesTab_files', parent=currentPanel).Hide()
                if modeString == 'files':
                    self.getGuiObj('TasteHeart').setBackground(wx.WHITE)
                """
                if modeString == 'library':
                    graph_parent = self.getAlternativeTabPanel('Tab_graphs', parent=currentPanel)
                    graph_parent.Hide()
                    #swap the dummy Graph panel with the plot panel
                    dummy_graph_panel = self.getGuiObj('Graph', 'Tab_graphs')
                    #optionsButton = self.getGuiObj('options')

                    emsg = None
                    try:
                        from graphs import StatsPanel
                        graph_panel = StatsPanel(graph_parent)
                    except ImportError, msg:
                        graph_panel = None
                        emsg=msg
                    if graph_panel is None:
                        def setData(item):
                            pass
                        dummy_graph_panel.setData = setData
                        def setVisible(isVisible):
                            pass
                        dummy_graph_panel.setVisible = setVisible
                        dummy_graph_panel.vSizer = wx.BoxSizer(wx.VERTICAL)
                        dummy_graph_panel.title =wx.StaticText(dummy_graph_panel,-1,"",wx.Point(0,0),wx.Size(300,300))        
                        dummy_graph_panel.title.SetBackgroundColour(wx.WHITE)
                        dummy_graph_panel.title.SetFont(wx.Font(10,FONTFAMILY,FONTWEIGHT,wx.NORMAL,False,FONTFACE))
                        dummy_graph_panel.title.SetMinSize((300,300))
                        dummy_graph_panel.vSizer.Add(dummy_graph_panel.title, 0, wx.BOTTOM, 3)
                        dummy_graph_panel.title.SetLabel(str(emsg))
                        dummy_graph_panel.SetSizer(dummy_graph_panel.vSizer);
                        dummy_graph_panel.SetAutoLayout(1);
                        dummy_graph_panel.Layout();
                        dummy_graph_panel.Refresh()
                    else:
                        self.swapPanel(dummy_graph_panel, graph_panel)
                        #also set it as an object of Tab_graphs
                        self.data[self.mode]['Tab_graphs'+'_'+'Graph'] = graph_panel
                        graph_panel.SetMinSize(wx.Size(300,300))
                        graph_panel.SetSize(wx.Size(300,300))
                """ 
                
                
            elif modeString in ['persons','friends']:
                self.getGuiObj('TasteHeart').setBackground(wx.WHITE)
                self.getGuiObj('info_detailsTab').setSelected(True)
                self.getGuiObj('advanced_detailsTab').SetLabel(" advanced")
                #get the list in the right mode for viewing
                self.setListAspect2OneColumn("alsoDownloadedField")
                self.setListAspect2OneColumn("commonFilesField")
                self.getAlternativeTabPanel('personsTab_advanced', parent=currentPanel).Hide()
                ofList = self.getGuiObj("alsoDownloadedField")
                cfList = self.getGuiObj("commonFilesField")
                ofList.setOtherList(cfList)
                ofList.setFieldsUpdateFunction(self.updateNumFilesInTextFields)
            
            elif modeString == "profile":
                self.data[self.mode]['profileDetails_Overall'] = currentPanel #also add first panel as an named element in the data list
#                self.item = "profileDetails_Overall" #the name of the panel that's currently selected
                self.getAlternativeTabPanel('profileDetails_Quality', parent=self).Hide() #parent is self because it is not a tab, it replaces the details panel
                self.getAlternativeTabPanel('profileDetails_Files', parent=self).Hide() #parent is self because it is not a tab, it replaces the details panel
                self.getAlternativeTabPanel('profileDetails_Persons', parent=self).Hide() #parent is self because it is not a tab, it replaces the details panel
                self.getAlternativeTabPanel('profileDetails_Download', parent=self).Hide() #parent is self because it is not a tab, it replaces the details panel
                self.getAlternativeTabPanel('profileDetails_Presence', parent=self).Hide() #parent is self because it is not a tab, it replaces the details panel                
                self.getAlternativeTabPanel('profileDetails_statsTopSharers', parent=self).Hide() #parent is self because it is not a tab, it replaces the details panel                
        return currentPanel
    
    def loadStatusPanel(self):
        currentPanel = self.loadXRCPanel(os.path.join(self.guiUtility.vwxGUI_path, 'statusDownloads.xrc'), 'statusDownloads')
        mode = 'status'
        for element in self.statdlElements:
            xrcElement = None
            name = None
            if type(element) == str:
                xrcElement = xrc.XRCCTRL(currentPanel, element)
                name = element
            elif type(element) == tuple:
                name = element[0]
                xrcElement = xrc.XRCCTRL(self.data[mode][element[1]],name)
            if not xrcElement:
                if DEBUG:
                    print >> sys.stderr,'standardDetails: Error: Could not identify xrc element: %s for mode %s' % (element, mode)
                pass
            if name:
                self.data[mode][name] = xrcElement                

        
        return currentPanel

    
    def loadXRCPanel(self, filename, panelName, parent=None):
        try:
            currentPanel = None
            if not os.path.exists(filename):
                dummyFile = os.path.join(self.guiUtility.vwxGUI_path, 'dummy.xrc')
                filename = dummyFile
                panelName = "dummy"
            res = xrc.XmlResource(filename)
            # create panel
            if parent is None:
                parent = self
            currentPanel = res.LoadPanel(parent, panelName)
            if not currentPanel:
                raise Exception()
            return currentPanel
        except:
            if DEBUG:
                print >> sys.stderr,'standardDetails: Error: Could not load panel from XRC-file %s' % filename
                print >> sys.stderr,'standardDetails: Tried panel: %s=%s' % (panelName, currentPanel)
            print_exc()
            return None
            
     
    def getData(self):
        return self.item
    
    def getIdentifier(self):
        if not self.item:
            return None
        try:
            if self.mode in ['filesMode','libraryMode']:
                return self.item['infohash']
            elif self.mode in ['personsMode','friendsMode']:
                return self.item['permid']
            elif self.mode in ['subscriptionsMode']:
                return self.item['url']
            elif DEBUG:
                print >> sys.stderr,'standardDetails: Error in getIdentifier for mode %s, item=%s' % (self.mode,self.item)
        except:
            if DEBUG:
                print >> sys.stderr,'standardDetails: Error in getIdentifier for mode %s, item=%s' % (self.mode,self.item)
                
            print_exc()
        
    def setData(self, item):
        self.updateCallback(item) # update callback function on changing item
        self.item = item
        if item is None:
            item = {}
        if self.mode in ['filesMode', 'libraryMode']:
            #check if this is a corresponding item from type point of view
            if item.get('infohash') is None:
                return #no valid torrent
            torrent = item
                        
            titleField = self.getGuiObj('titleField')
            title = torrent.get('name')
            title = title[:77]
            titleField.SetLabel(title)
            titleField.Wrap(-1) # doesn't appear to work
            
            self.setTorrentThumb(self.mode, torrent, self.getGuiObj('thumbField'))        
            
            if self.getGuiObj('info_detailsTab').isSelected():
                # The info tab is selected, show normal torrent info
                
                self.setDownloadbutton(torrent)
                
                descriptionField = self.getGuiObj('descriptionField')

                descrtxt = ''
                flag = False
                if not torrent.get('web2'):
                    if 'metadata' in torrent:
                        metadata = torrent['metadata']

                        encoding = None
                        if 'encoding' in metadata and metadata['encoding'].strip():
                            encoding = metadata['encoding']

                        flag = False
                        for key in ['comment','comment-utf8','Description']: # reverse priority
                            if key in metadata: # If vuze torrent
                                tdescrtxt = metadata[key]
                                if key == 'comment-utf8':
                                    tencoding = 'utf_8'
                                else:
                                    tencoding = encoding
                                descrtxt = bin2unicode(tdescrtxt,tencoding)
                                flag = True
                        if not flag:
                            if 'source' in torrent:
                                s = torrent['source']
                                if s != '':
                                    if s == 'BC':
                                        s = 'Received from other user'
                                    descrtxt = "Source: "+s

                                flag = True
                else:
                    descrtxt = torrent['description']
                    flag = True
                 
                if not flag:
                    if 'source' in torrent:
                        s = torrent['source']
                        if s == 'BC':
                            s = 'Received from other user'
                        descrtxt = "Source: "+s

                descriptionField.SetLabel(descrtxt)
                descriptionField.Wrap(-1)        
    
                sizeField = self.getGuiObj('sizeField')
                if not torrent.get('web2'):
                    sizeField.SetLabel(self.utility.size_format(torrent['length']))
                else:
                    sizeField.SetLabel(torrent['length'])

                creationField = self.getGuiObj('creationdateField')
                if torrent.get('creation_date',0):
                    creationField.SetLabel(friendly_time(torrent['creation_date']))
                else:
                    creationField.SetLabel('?')
                    
                if torrent.get('web2'):
                    #view = self.getGuiObj('views')
                    #view.Show()
                    #pop = self.getGuiObj('popularity')
                    #pop.Hide()
                    #pop.GetParent().Layout()

                    viewsField = self.getGuiObj('popularityField1')
                    viewsField.SetLabel(str(torrent['views']) + " views")
                    viewsField.SetToolTipString('')
                    
                    self.getGuiObj('popularityField2').Hide()
                    self.getGuiObj('up').Hide()                    
                    self.getGuiObj('down').Hide()
                    self.getGuiObj('refresh').Hide()
                    

                    viewsField.GetParent().Layout()
                    viewsField.SetSize((100,18))

                else:

                    self.getGuiObj('popularityField2').Show()
                    self.getGuiObj('up').Show()
                    self.getGuiObj('down').Show()
                    self.getGuiObj('refresh').Show()

                    if torrent.has_key('num_seeders'):
                        seeders = torrent['num_seeders']
                        seedersField = self.getGuiObj('popularityField1')
                        leechersField = self.getGuiObj('popularityField2')
                        
                        if seeders > -1:
                            seedersField.SetLabel('%d' % seeders)
                            seedersField.SetToolTipString(self.utility.lang.get('seeder_tool') % seeders)
                            self.getGuiObj('up').SetToolTipString(self.utility.lang.get('seeder_tool') % seeders)
                            leechersField.SetLabel('%d' % torrent['num_leechers'])
                            self.getGuiObj('down').SetToolTipString(self.utility.lang.get('leecher_tool') % torrent['num_leechers'])
                            leechersField.SetToolTipString(self.utility.lang.get('leecher_tool') % torrent['num_leechers'])
                            
                        else:
                            seedersField.SetLabel('?')
                            seedersField.SetToolTipString('')
                            leechersField.SetLabel('?')
                            leechersField.SetToolTipString('')
                            self.getGuiObj('up').SetToolTipString('')
                            self.getGuiObj('down').SetToolTipString('')
                            seedersField.SetSize((36,18))
                            
                        refreshString = '%s: %s' % (self.utility.lang.get('last_checked'), friendly_time(torrent.get('last_check_time')))
                        self.getGuiObj('refresh').SetToolTipString(refreshString)
                    seedersField.GetParent().Layout()
                        
                
                # Call a function to retrieve similar torrent data
                wx.CallAfter(self.fillSimLists, item)
                #wx.CallAfter(self.fillSimTorrentsList, item['infohash'])
                #wx.CallAfter(self.fillSimTitlesList, item)
                # Show or hide download button in detailstab
                if self.mode == 'filesMode':
                        
                    # Set tastheart and ranking
                    rank = torrent.get('simRank', -1)
                    self.getGuiObj('TasteHeart').setRank(rank)
                    self.setRankToRecommendationField(rank)
                
            elif self.getGuiObj('files_detailsTab').isSelected():
                tab = 'filesTab_files'
                filesList = self.getGuiObj('includedFiles', tab = tab)
                filesList.setData(torrent)
                self.getGuiObj('filesField', tab = tab).SetLabel('%d' % filesList.getNumFiles())
                # Remove download button for libraryview
                self.setDownloadbutton(torrent, tab = tab)
                
                # Set tracker info
                trackerField = self.getGuiObj('trackerField', tab = tab)
                trackerField.Wrap(-1)
                if torrent.has_key('tracker'):
                    trackerString = torrent['tracker']
                    short = getShortTrackerFormat(trackerString)
                    trackerField.SetLabel(short)
                    trackerField.SetToolTipString(trackerString)
                else:
                    trackerField.SetLabel('')
                    trackerField.SetToolTipString('')
                    
            #elif self.getGuiObj('graphs_detailsTab').isSelected():
            #    if DEBUG:
            #        print "standardDetails: graph set data"
            #    graph_panel = self.getGuiObj("Graph", "Tab_graphs")
            #    if graph_panel is not None:
            #        graph_panel.setData(item)
            elif DEBUG:
                print >> sys.stderr,'standardDetails: error: unknown tab selected'
            
                        
        elif self.mode in ['personsMode', 'friendsMode']:
            #check if this is a corresponding item from type point of view
#            if item.get('permid') is None:
#                return #no valid torrent
            
            titleField = self.getGuiObj('titleField')
            titleField.SetLabel(item.get('name',''))
            titleField.Wrap(-1)
            
            #set the picture
            try:
                bmp = None
                # Check if we have already read the thumbnail and metadata information from this torrent file
                if item.get('metadata'):
                    bmp = item['metadata'].get('ThumbnailBitmap')
                else:
                    mime, icondata = self.peer_db.getPeerIcon(item['permid'])
                    if icondata:
                        bmp = data2wxBitmap(mime,icondata)
                        
                if not bmp:
                    superpeers = self.superpeer_db.getSuperPeers()
                    if item['permid'] in superpeers:
                        bmp = self.iconsManager.get_default('personsMode','SUPERPEER_BITMAP')
                    else:
                        bmp = self.iconsManager.get_default('personsMode','DEFAULT_THUMB')
                
                thumbField = self.getGuiObj("thumbField")
                thumbField.setBitmap(bmp)
                width, height = thumbField.GetSize()
                d = 1
                thumbField.border = [wx.Point(0,d), wx.Point(width-d, d), wx.Point(width-d, height-d), wx.Point(d,height-d), wx.Point(d,0)]
                thumbField.Refresh()
#                wx.CallAfter(thumbField.Refresh)
                
            except:
                print_exc()
            

            if self.getGuiObj('info_detailsTab').isSelected():
                
                if item.get('simRank'):
                    if DEBUG:
                        print >> sys.stderr, 'SimRank of peer: %s' % item['simRank']
                    self.setRankToRecommendationField(item['simRank'])
                    self.getGuiObj('TasteHeart').setRank(item['simRank'])
                
                if item.get('online'):
                    self.getGuiObj('statusField').SetLabel( 'online')
                elif item.get('last_connected') is not None:
                    if item['last_connected'] < 0:
                        self.getGuiObj('statusField').SetLabel('never seen')
                    else:
                        self.getGuiObj('statusField').SetLabel('conn.  %s' % friendly_time(item['last_connected']))
                else:
                    self.getGuiObj('statusField').SetLabel( 'unknown')

                if 'npeers' in item:
                    n = unicode(item['npeers'])
                    if not n or n=='0':
                        n = '?'
                    self.getGuiObj('discPersonsField').SetLabel(n)
                if 'ntorrents' in item:
                    n = unicode(item['ntorrents'])
                    if not n or n == '0':
                        n = '?'
                    self.getGuiObj('discFilesField').SetLabel(n)
                
                if item.get('friend') is not None:
                    if item['friend']:
                        isfriend = self.iconsManager.get_default('personsMode','ISFRIEND_BITMAP')
                        isfriend_clicked = self.iconsManager.get_default('personsMode','ISFRIEND_CLICKED_BITMAP')
                        self.getGuiObj('addAsFriend').switchTo(isfriend,isfriend_clicked)
                    else:
                        self.getGuiObj('addAsFriend').switchBack()
                    
                self.fillTorrentLists()
                
            elif self.getGuiObj('advanced_detailsTab').isSelected():
                if item.get('last_connected') is not None:
                    if item['last_connected'] < 0:
                        self.getGuiObj('lastExchangeField', tab = 'personsTab_advanced').SetLabel("never seen online")
                    else:
                        self.getGuiObj('lastExchangeField', tab = 'personsTab_advanced').SetLabel('%s %s'%(friendly_time(item['last_connected']),'ago'))
                else:
                    self.getGuiObj('lastExchangeField', tab = 'personsTab_advanced').SetLabel('')
                if item.get("connected_times") is not None:
                    self.getGuiObj('timesConnectedField', tab = 'personsTab_advanced').SetLabel(str(item["connected_times"]))
                else:
                    self.getGuiObj('timesConnectedField', tab = 'personsTab_advanced').SetLabel("")
                if item.get("similarity") is not None:
                    self.getGuiObj('similarityValueField', tab = 'personsTab_advanced').SetLabel("%.1f" % item["similarity"])
                else:
                    self.getGuiObj('similarityValueField', tab = 'personsTab_advanced').SetLabel("")
                
                addAsFriend = self.getGuiObj('addAsFriend', tab = 'personsTab_advanced')
                if addAsFriend.initDone:
                    if item.get('friend') is not None:
                        if item['friend']:
                            isfriend = self.iconsManager.get_default('personsMode','ISFRIEND_BITMAP')
                            isfriend_clicked = self.iconsManager.get_default('personsMode','ISFRIEND_CLICKED_BITMAP')
                            addAsFriend.switchTo(isfriend,isfriend_clicked)
                        else:
                            addAsFriend.switchBack()
            
        elif self.mode == 'subscriptionsMode':
            if item.get('url') is None:
                return #no valid url
            subscrip = item
            rssurl = subscrip.get('url')
            
            if self.subscr_old_source is not None and self.subscr_old_source == rssurl:
                if DEBUG:
                    print >>sys.stderr,"standardDetails: setData: subscriptionMode: Not refreshing"
                return # no need to refresh
            self.subscr_old_source = rssurl
            
            titleField = self.getGuiObj('titleField')
            titleField.SetLabel(rssurl)
            titleField.Wrap(-1)

            bcsub = self.utility.lang.get('buddycastsubscription')
            if rssurl == bcsub:
                rssurl = 'BC'
            
            # Gather data for views
            torrents = self.torrent_db.getTorrentsFromSource(rssurl)
            todayl = []
            yesterdayl = []
            now = long(time())
            sotoday = long(math.floor(now / (24*3600.0))*24*3600.0)
            soyester = long(sotoday - (24*3600.0))
            for torrent in torrents:
                    if torrent['insert_time'] > sotoday:
                        todayl.append(torrent)
                    elif torrent['insert_time'] > soyester:
                        yesterdayl.append(torrent)
            
            todayl.sort(reverse_torrent_insertime_cmp)
            yesterdayl.sort(reverse_torrent_insertime_cmp)
            
            # Update Today view
            todayField = self.getGuiObj('receivedToday')
            todaystr = "   Today ("+str(len(todayl))+")"
            todayField.SetLabel(todaystr)

            todayList = self.getGuiObj('subscrTodayField')
            if sys.platform == 'win32':
                todayList.SetWindowStyleFlag(wx.LC_REPORT|wx.NO_BORDER|wx.LC_NO_HEADER|wx.LC_SINGLE_SEL) #it doesn't work on mac
            else:
                todayList.SetSingleStyle(wx.NO_BORDER)
                todayList.SetSingleStyle(wx.LC_REPORT)
                todayList.SetSingleStyle(wx.LC_NO_HEADER)
                todayList.SetSingleStyle(wx.LC_SINGLE_SEL)
#            todayList.SetWindowStyle(wx.LC_REPORT|wx.NO_BORDER|wx.LC_SINGLE_SEL|wx.LC_NO_HEADER)
            if todayList.GetColumnCount() == 0:
                todayList.InsertColumn(0, "Torrent",wx.LIST_FORMAT_LEFT,280)
            todayList.DeleteAllItems()
            
            today_infohashes = []
            for torrent in todayl:
                todayList.Append([torrent['name']])
                today_infohashes.append(torrent['infohash'])
            todayList.setInfoHashList(today_infohashes)

            # Update Yesterday view
            ydayField = self.getGuiObj('receivedYesterday')
            ydaystr = "   Yesterday ("+str(len(yesterdayl))+")"
            ydayField.SetLabel(ydaystr)

            ydayList = self.getGuiObj('subscrYesterdayField')
            if sys.platform == 'win32':
                ydayList.SetWindowStyleFlag(wx.LC_REPORT|wx.NO_BORDER|wx.LC_NO_HEADER|wx.LC_SINGLE_SEL) #it doesn't work on mac
            else:
                ydayList.SetSingleStyle(wx.NO_BORDER)
                ydayList.SetSingleStyle(wx.LC_REPORT)
                ydayList.SetSingleStyle(wx.LC_NO_HEADER)
                ydayList.SetSingleStyle(wx.LC_SINGLE_SEL)
            if ydayList.GetColumnCount() == 0:
                ydayList.InsertColumn(0, "Torrent",wx.LIST_FORMAT_LEFT,280)
            ydayList.DeleteAllItems()
            yesterday_infohashes = []
            for torrent in yesterdayl:
                ydayList.Append([torrent['name']])
                yesterday_infohashes.append(torrent['infohash'])
            ydayList.setInfoHashList(yesterday_infohashes)
        
        elif self.mode == 'profileMode':
            if len(item) == 0:
                return
            tab = None

            # --------------------------------------------------------------------------------------------------------------------------------------------------------
            ## --- Overall performance  !!!! we'll leave it probably out!!!
            if self.currentPanel == self.getGuiObj('profileDetails_Overall'):  
#                self.getGuiObj('descriptionField0').SetLabel(item.get('overall_rank'))            
                picture = self.getGuiObj("levelPic")                
                if item.get('overall_rank') == "beginner": 
                    picture.setIndex(0)
                if item.get('overall_rank') == "experienced": 
                    picture.setIndex(1)
                if item.get('overall_rank') == "top user": 
                    picture.setIndex(2)
                if item.get('overall_rank') == "master": 
                    picture.setIndex(3)
                               
            # --------------------------------------------------------------------------------------------------------------------------------------------------------            
            # --- Quality of tribler recommendations    
            elif self.currentPanel == self.getGuiObj('profileDetails_Quality'):
                tab = 'profileDetails_Quality'
                count = item.get('downloaded_files',0)            
                text = self.utility.lang.get("profileDetails_Quality_description", giveerror=False)
                text1 = self.utility.lang.get("profileDetails_Quality_improve", giveerror=False)
                if count < 10:
                    only = self.utility.lang.get("profileDetails_Quality_description_onlyword", giveerror=False)
                else:
                    only=""
                self.getGuiObj('descriptionField0', tab = 'profileDetails_Quality').SetLabel(text % (only,count))
                self.getGuiObj('descriptionField1', tab = 'profileDetails_Quality').SetLabel(text1)

            # --------------------------------------------------------------------------------------------------------------------------------------------------------
            # --- Discovered Files
            elif self.currentPanel == self.getGuiObj('profileDetails_Files'):  
                tab = 'profileDetails_Files'              
                count = item.get('discovered_files',0)
                count2 = self.utility.config.Read('maxntorrents','int')
                text = self.utility.lang.get("profileDetails_Files_description", giveerror=False)
                text1 = self.utility.lang.get("profileDetails_Files_improve", giveerror=False)
                self.getGuiObj('descriptionField0', tab = 'profileDetails_Files').SetLabel(text % count)
                self.getGuiObj('descriptionField1', tab = 'profileDetails_Files').SetLabel(text1 % count2)  
                          
            # --------------------------------------------------------------------------------------------------------------------------------------------------------
            # --- Discovered Persons
            elif self.currentPanel == self.getGuiObj('profileDetails_Persons'):
                tab = 'profileDetails_Persons'
                count = 0 
                count = item.get('discovered_persons',0) 
                text = self.utility.lang.get("profileDetails_Persons_description", giveerror=False)
                text1 = self.utility.lang.get("profileDetails_Persons_improve", giveerror=False)
                self.getGuiObj('descriptionField0', tab = 'profileDetails_Persons').SetLabel(text % count)
                self.getGuiObj('descriptionField1', tab = 'profileDetails_Persons').SetLabel(text1)  

            # --------------------------------------------------------------------------------------------------------------------------------------------------------
            ## --- Optimal download speed    
            elif self.currentPanel == self.getGuiObj('profileDetails_Download'):    
                tab = 'profileDetails_Download'
                text = self.utility.lang.get("profileDetails_Download_info", giveerror=False)
                self.getGuiObj('descriptionField', tab = 'profileDetails_Download').SetLabel(text)

                maxuploadrate = self.guiUtility.utility.config.Read('maxuploadrate', 'int') #kB/s
                if ( maxuploadrate == 0 ):
                    text1 = self.utility.lang.get("profileDetails_Download_UpSpeedMax", giveerror=False)
                    text2 = self.utility.lang.get("profileDetails_Download_UpSpeedMax_improve", giveerror=False)
                else:
                    text1 = self.utility.lang.get("profileDetails_Download_UpSpeed", giveerror=False)
                    text1 = text1 % maxuploadrate                    
                    text2 = self.utility.lang.get("profileDetails_Download_UpSpeed_improve", giveerror=False)
    #            maxuploadslots = self.guiUtility.utility.config.Read('maxupload', "int")
    #            if ( maxuploadslots == 0 ):
    #                text2 = self.utility.lang.get("profileDetails_Download_UpSlotsMax", giveerror=False)
    #            else:
    #                text2 = self.utility.lang.get("profileDetails_Download_UpSlots", giveerror=False)
    #                text2 = text2 % maxuploadslots
    #            maxdownloadrate = self.guiUtility.utility.config.Read('maxdownloadrate', "int")
    #            if ( maxdownloadrate == 0 ):
    #                text3 = self.utility.lang.get("profileDetails_Download_DlSpeedMax", giveerror=False)
    #            else:
    #                text3 = self.utility.lang.get("profileDetails_Download_DlSpeed", giveerror=False)
    #                text3 = text3 % maxdownloadrate
    #            text = "%s\n%s\n%s" % (text1,text2,text3)
                self.getGuiObj('descriptionField0', tab = 'profileDetails_Download').SetLabel( text1)            
                self.getGuiObj('descriptionField1', tab = 'profileDetails_Download').SetLabel(text2)

                count = item.get('number_friends',0)
                text = self.utility.lang.get("profileDetails_Download_Friends", giveerror=False)
                self.getGuiObj('descriptionField2', tab = 'profileDetails_Download').SetLabel(text % count)
                text = self.utility.lang.get("profileDetails_Download_Friends_improve", giveerror=False)
                self.getGuiObj('descriptionField3', tab = 'profileDetails_Download').SetLabel(text)

                nat =  item.get('nat_type') 
                if self.guiUtility.isReachable():
                    text1 = self.utility.lang.get("profileDetails_Download_VisibleYes", giveerror=False)
                    text2 = self.utility.lang.get("profileDetails_Download_VisibleYes_improve", giveerror=False)
                    self.getGuiObj('descriptionField4', tab = 'profileDetails_Download').SetLabel(text1)
                    self.getGuiObj('descriptionField5', tab = 'profileDetails_Download').SetLabel(text2)
                else:
                    text1 = self.utility.lang.get("profileDetails_Download_VisibleNo", giveerror=False)
                    text2 = self.utility.lang.get("profileDetails_Download_VisibleNo_improve", giveerror=False)
                    self.getGuiObj('descriptionField4', tab = 'profileDetails_Download').SetLabel(text1 % nat)
                    self.getGuiObj('descriptionField5', tab = 'profileDetails_Download').SetLabel(text2)

            # --------------------------------------------------------------------------------------------------------------------------------------------------------        
            ## --- Reachability
            elif self.currentPanel == self.getGuiObj('profileDetails_Presence'):    
                tab = 'profileDetails_Presence'
                text = self.utility.lang.get("profileDetails_Presence_info", giveerror=False)
                self.getGuiObj('descriptionField', tab = 'profileDetails_Presence').SetLabel(text)
                
                count = item.get('number_friends',0)
                # use text that is also used in 'optimal download details        
                text = self.utility.lang.get("profileDetails_Download_Friends", giveerror=False)
                self.getGuiObj('descriptionField0', tab = 'profileDetails_Presence').SetLabel(text % count)
                text = self.utility.lang.get("profileDetails_Download_Friends_improve", giveerror=False)
                self.getGuiObj('descriptionField1', tab = 'profileDetails_Presence').SetLabel(text)
                
                current_version = self.utility.getVersion()
                text = self.utility.lang.get("profileDetails_Presence_VersionUnknown", giveerror=False)
                new_version = item.get('new_version',text)
                update_url = 'www.tribler.org' #item.get('update_url','www.tribler.org')
                compare_result = item.get('compare_result',-3)
                if compare_result == -1: #newer version locally
                    text1 = self.utility.lang.get("profileDetails_Presence_VersionNewer", giveerror=False)
                    text1 = text1 % (current_version, new_version)
                    text2 = self.utility.lang.get("profileDetails_Presence_VersionNewer_improve", giveerror=False)
                    text2 = text2 % update_url
                elif compare_result == 0: #same version
                    text1 = self.utility.lang.get("profileDetails_Presence_VersionCurrent", giveerror=False)
                    text1 = text1 % current_version
                    text2 = self.utility.lang.get("profileDetails_Presence_VersionCurrent_improve", giveerror=False)
                    text2 = text2 % update_url
                elif compare_result == 1: #newer version on website
                    text1 = self.utility.lang.get("profileDetails_Presence_VersionOlder", giveerror=False)
                    text1 = text1 % current_version
                    text2 = self.utility.lang.get("profileDetails_Presence_VersionOlder_improve", giveerror=False)
                    text2 = text2 % (new_version,update_url)
                else:
                    text1 = self.utility.lang.get("profileDetails_Presence_VersionError", giveerror=False)
                    text1 = text1 % current_version
                    text2 = self.utility.lang.get("profileDetails_Presence_VersionError_improve", giveerror=False)
                    text2 = text2 % update_url
                self.getGuiObj('descriptionField4', tab = 'profileDetails_Presence').SetLabel(text1)
                self.getGuiObj('descriptionField5', tab = 'profileDetails_Presence').SetLabel(text2)
            # --------------------------------------------------------------------------------------------------------------------------------------------------------
            # --- Top N List of sharers
            elif self.currentPanel == self.getGuiObj('profileDetails_statsTopSharers'):
                tab = 'profileDetails_statsTopSharers'
                self.topNListText(tab)
                self.refreshStandardDetailsHeight()
                
            else:
                tab = "error"
            if tab != "error":
                if self.reHeightToFit(tab):
                    
                    #print "<mluc> do panel ",tab,"relayouting"
                    self.currentPanel.SetAutoLayout(1)
                    self.currentPanel.Layout()
                    self.hSizer.Layout()
        elif DEBUG:
            print >> sys.stderr,"standardDetails: setData: No entry for mode",self.mode
                    
#        self.currentPanel.Refresh()
    
    def setRankToRecommendationField(self, rank):
        recommField = self.getGuiObj('recommendationField')
        assert recommField, "No recommendationField found"
            
        if rank != -1:
            
            if rank == 1:
                recommField.SetLabel("%d" % rank + "st of top 20")
            elif rank == 2:
                recommField.SetLabel("%d" % rank + "nd of top 20")                        
            elif rank == 3:
                recommField.SetLabel("%d" % rank + "rd of top 20")
            else:
                recommField.SetLabel("%d" % rank + "th of top 20")
        else:
            recommField.SetLabel("")
        
    def reHeightToFit(self, tab=None):
        """the idea is to iterate through all objects mentioned in the list of 
        object for current tab and to reposition them on y axis so that all of
        them are fully visible -> update, the repositioning should be done automatically by 
        autolayouting the sizer, all that has to be done is wrap the elements that don't
        have the ST_NO_AUTORESIZE flag set
        returns true if elements have been repositioned so that the layout be redone"""
#        if DEBUG:
#            print >> sys.stderr,"standardDetails: <mluc> trying to reheight panel for mode",self.mode,"and tab",tab
        bElementMoved = False
        VERTICAL_SPACE = 3
        try:
            if tab is None:
                list = self.modeElements[self.mode]
            else:
                list = self.tabElements[tab]
            #check to see it it's worth trying to reposition elements
            if len(list)>0:
                prevElement = None
                for elementName in list:
                    currentElement = self.getGuiObj(elementName, tab)
                    if isinstance(currentElement,wx.StaticText):
                        style = currentElement.GetWindowStyle()
#                        if DEBUG:
#                            print >> sys.stderr,"standardDetails: <mluc> element",elementName,"has style",style
                        #print 'Style if %s has flag: %s' % (elementName, (style & wx.ST_NO_AUTORESIZE))
                        if (style & wx.ST_NO_AUTORESIZE)==0 :
                            currentElement.Wrap(284)
                            bElementMoved = True
                    prevElement = None
                    if prevElement is not None:
                        prevPos = prevElement.GetPosition().y
                        prevHeight = prevElement.GetSize().height
                        new_pos = prevPos + prevHeight + VERTICAL_SPACE
    #                    print "<mluc> element",list[index],"is at",currentElement.GetPosition().y,"and has height",currentElement.GetSize().height
                        if new_pos != currentElement.GetPosition().y:
                            #reposition element as it overlaps the one above
                            currentElement.SetPosition(wx.Point(currentElement.GetPosition().x,new_pos))
                    prevElement = currentElement
            # Set size of standardDetails to size of content
            
            
        except:
            print_exc()
        return bElementMoved
    
    def setDownloadbutton(self, torrent, tab = None):
        downloadButton = self.getGuiObj('download', tab = tab)
        if downloadButton:
            if torrent.get('myDownloadHistory', False):
                bitmap, bitmap2 = self.iconsManager.getDownloadButton('library')
            elif torrent.get('web2'):
                bitmap, bitmap2 = self.iconsManager.getDownloadButton('play')
            else:
                bitmap, bitmap2 = self.iconsManager.getDownloadButton('download')
            downloadButton.setBitmaps(bitmap, bitmap2)
                 
    def getGuiObj(self, obj_name, tab=None, mode=None):
        """handy function to retreive an object based on it's name for the current mode"""
        if tab:
            obj_name = tab+'_'+obj_name
        if not mode:
            mode = self.mode
        #print 'Available objects: %s' % self.data[mode].keys()
        return self.data[mode].get(obj_name)
     
    def show_loading(self, list_obj):
        list_obj.DeleteAllItems()
        index = list_obj.InsertStringItem(sys.maxint, "Searching..")
        font = list_obj.GetItemFont(index)
        font.SetStyle(wx.FONTSTYLE_ITALIC)
        list_obj.SetItemFont(index, font)
        list_obj.SetItemTextColour(index, "#555555")
     
    def fillSimLists(self, item):
        # show loading..
        self.show_loading(self.getGuiObj('peopleWhoField'))
        self.show_loading(self.getGuiObj('simTitlesField'))
        
        overlay_bridge = OverlayThreadingBridge.getInstance()
        overlay_bridge.add_task(lambda:self.updateSimLists(item), id='fillSimLists')

    def updateSimLists(self, item):
        def cmpfunc(x, y):
            return int(10000*(editDist(x[1], name) - editDist(y[1], name)))
        
        infohash = item['infohash']
        name = item['name']
        gui_db = GUIDBHandler.getInstance()
        
        sim_files = None
        sim_titles = None
        
        try:
            sim_files = gui_db.getSimItems(infohash, 8)
        except:
            print_exc()
        wx.CallAfter(self.fillSimTorrentsList, sim_files)

        try:
            sim_titles = gui_db.getSimilarTitles(name, 30, infohash)  # first get a subset of titles
            sim_titles.sort(cmpfunc)
        except:
            print_exc()
        wx.CallAfter(self.fillSimTitlesList, sim_titles)
     
    def fillSimTorrentsList(self, sim_files):
        """fills the list of torrents from library or file view with the files that are similar to the currently selected one"""
        # jie.done: fill similar torrent list
        # future.work: smooth the recommendation, solve the data sparse and cold start problem
        
        sim_torrent_list = self.getGuiObj('peopleWhoField')
        if not sim_torrent_list:    # user already switched to another page
            return
        sim_torrent_list.DeleteAllItems()
        if sim_files is None:
            self.errorLoadData('peopleWhoField')
            return
        
        try:
            #sim_files = self.gui_db.getSimItems(infohash, 8)   # [(infohash, title)]
            sim_torrent_list.setInfoHashList(None)
        
            torrent_list = []
            if len(sim_files) > 0:
                for infohash, name, status_id, coocurrence in sim_files:
                    if coocurrence <= 1:    # don't show too irrelevant torrents. set it to 0 if you want to show all co-occurent torrents
                        continue
                    if status_id == 0:  # good
                        color = "blue"
                    elif status_id == 1:  # unknown
                        color = "black"
                    elif status_id == 2:  # dead
                        color = "red"
                        continue
                    index = sim_torrent_list.InsertStringItem(sys.maxint, name)
                    sim_torrent_list.SetItemTextColour(index, color)
                    torrent_list.append(infohash)
                sim_torrent_list.setInfoHashList(torrent_list)

            if len(torrent_list) == 0:
                index = sim_torrent_list.InsertStringItem(sys.maxint, "No similar files found yet.")
                font = sim_torrent_list.GetItemFont(index)
                font.SetStyle(wx.FONTSTYLE_ITALIC)
                sim_torrent_list.SetItemFont(index, font)
                sim_torrent_list.SetItemTextColour(index, "#222222")
                
        except Exception, e:
            print_exc()
            sim_torrent_list.setInfoHashList(None)
            index = sim_torrent_list.InsertStringItem(0, "Error getting similar files list")
            sim_torrent_list.SetItemTextColour(index, "dark red")
            
        try:
            sim_torrent_list.onListResize() #SetColumnWidth(0,wx.LIST_AUTOSIZE)
        except:
            if DEBUG:
                print >> sys.stderr,"standardDetails: could not resize lists in sim_torrent_list panel" 
        

    def fillSimTitlesList(self, sim_titles):
        """fills the list of torrents with similar titles"""
        # jie.done: fill sim title list
        
        sim_torrent_list = self.getGuiObj('simTitlesField')
        if not sim_torrent_list:
            return
        sim_torrent_list.DeleteAllItems()
        
        if sim_titles is None:
            self.errorLoadData('simTitlesField')
            return
        
        try:
            sim_torrent_list.setInfoHashList(None)
            
            torrent_list = []
            if len(sim_titles) > 0:
                for infohash, name, status_id in sim_titles:
                    #if infohash == item['infohash']:
                    #    continue
                    index = sim_torrent_list.InsertStringItem(sys.maxint, name)
                    if status_id == 0:  # good
                        color = "blue"
                    elif status_id == 1:  # unknown
                        color = "black"
                    elif status_id == 2:  # dead
                        color = "red"
                        continue
                    sim_torrent_list.SetItemTextColour(index, color)
                    torrent_list.append(infohash)
                sim_torrent_list.setInfoHashList(torrent_list)
            
            if len(torrent_list) == 0:
                index = sim_torrent_list.InsertStringItem(sys.maxint, "No similar files found yet.")
                font = sim_torrent_list.GetItemFont(index)
                font.SetStyle(wx.FONTSTYLE_ITALIC)
                sim_torrent_list.SetItemFont(index, font)
                sim_torrent_list.SetItemTextColour(index, "#222222")
            
        except Exception, e:
            print_exc()
            sim_torrent_list.setInfoHashList(None)
            index = sim_torrent_list.InsertStringItem(0, "Error getting similar files list")
            sim_torrent_list.SetItemTextColour(index, "dark red")
        
        try:
            sim_torrent_list.onListResize() #SetColumnWidth(0,wx.LIST_AUTOSIZE)
        except:
            if DEBUG:
                print >> sys.stderr,"standardDetails: could not resize lists in sim_torrent_list panel" 
        

    def fillTorrentLists(self):
        """fills the lists of torrents from persons detail view with common 
        and history files for the selected person"""
        
        ofList = self.getGuiObj("alsoDownloadedField")
        if not ofList:
            return 
        cfList = self.getGuiObj("commonFilesField")
        if not cfList:
            return

        ofList.setInfoHashList(None)
        if ( self.mode != "personsMode" and self.mode != "friendsMode" ) or \
            self.item is None or self.item.get('permid') is None:
            return

        self.show_loading(ofList)
        self.show_loading(cfList)

        overlay_bridge = OverlayThreadingBridge.getInstance()
        permid = self.item.get('permid')
        overlay_bridge.add_task(lambda:self.updateTorrentLists(permid), id='fillTorrentLists')

    def updateTorrentLists(self, permid):
        common_files = None
        other_files = None
        gui_db = GUIDBHandler.getInstance()

        try:
            common_files = gui_db.getCommonFiles(permid)  #[name]
        except:
            print_exc()
        wx.CallAfter(self.fillCommonList, common_files)
            
        try:
            other_files = gui_db.getOtherFiles(permid)    #[(infohash,name)] 
        except:
            print_exc()
        wx.CallAfter(self.fillOtherList, other_files)
            
    def fillCommonList(self, common_files):
        cfList = self.getGuiObj("commonFilesField")
        cfList.DeleteAllItems()
        
        if common_files is None:
            self.errorLoadData('commonFilesField')
            return
        
        if len(common_files) == 0:
            index = cfList.InsertStringItem(sys.maxint, "No common files with this person.")
            font = cfList.GetItemFont(index)
            font.SetStyle(wx.FONTSTYLE_ITALIC)
            cfList.SetItemFont(index, font)
            cfList.SetItemTextColour(index, "#222222")
            cfList.isEmpty = True    # used by DLFilesList to remove "No common files with this person."
        else:
            cfList.isEmpty = False
            for name in common_files:
                cfList.InsertStringItem(sys.maxint, name)
            
    def fillOtherList(self, other_files):
        ofList = self.getGuiObj("alsoDownloadedField")
        ofList.DeleteAllItems()
        
        if other_files is None:
            self.errorLoadData('alsoDownloadedField')
            return
        
        if len(other_files) == 0:
            index = ofList.InsertStringItem(sys.maxint, "No files advertised by this person.")
            font = ofList.GetItemFont(index)
            font.SetStyle(wx.FONTSTYLE_ITALIC)
            ofList.SetItemFont(index, font)
            ofList.SetItemTextColour(index, "#222222")
        else:
            torrent_list = []
            for infohash, name in other_files:
                ofList.InsertStringItem(sys.maxint, name)
                torrent_list.append(infohash)
            ofList.setInfoHashList(torrent_list)
                
    def errorLoadData(self, obj_name):
        flist = self.getGuiObj(obj_name)
        flist.DeleteAllItems()
        if isinstance(flist, DLFilesList):
            flist.setInfoHashList(None)      
        index = flist.InsertStringItem(sys.maxint, "Error: cannot load the data")
        flist.SetItemTextColour(index, "#222222")
        
        try:
            flist.onListResize()
        except:
            if DEBUG:
                print >> sys.stderr,"standardDetails: could not resize lists in person detail panel"
        
    def updateNumFilesInTextFields(self, cfList, ofList):
        numItems = [cfList.GetItemCount(), ofList.GetItemCount()]
        self.getGuiObj('commonFiles').SetLabel(self.utility.lang.get('commonFiles') % numItems[0])
        nprefs = max(self.getData().get('nprefs',0), numItems[1])
        self.getGuiObj('alsoDownloaded').SetLabel(self.utility.lang.get('alsoDownloaded') % (numItems[1], nprefs))
            
    def checkGraphTabVisible(self, tab2check='Graph', selectedTab=None):
        # just some generic way of making sure that a certain panel is informed when it is or not visible
        #the function must be there!
        graph_panel = self.getGuiObj(obj_name='Graph', tab='Tab_graphs', mode='libraryMode')
        if graph_panel is None:
            return
        if self.mode == 'libraryMode':
            if selectedTab is None:
                #find currently selected tab
                tabButtons = { 'files_detailsTab':self.getGuiObj('files_detailsTab'),
                              'info_detailsTab':self.getGuiObj('info_detailsTab'),
                              'graphs_detailsTab':self.getGuiObj('graphs_detailsTab') }
                for key in tabButtons.keys():
                    if tabButtons[key].isSelected():
                        selectedTab = key
                        break
            if selectedTab == 'graphs_detailsTab':
                graph_panel.setVisible(True)
                return
        graph_panel.setVisible(False)
    
    def tabClicked(self, name):
        if DEBUG:
            print >> sys.stderr,'standardDetails: tabClicked: %s' % name
        #self.checkGraphTabVisible(selectedTab=name)

        if self.mode == 'libraryMode':
            tabButtons = { 'files_detailsTab':self.getGuiObj('files_detailsTab'),
                          'info_detailsTab':self.getGuiObj('info_detailsTab')}
                          # 'graphs_detailsTab':self.getGuiObj('graphs_detailsTab') }
            tabPanelNames = { 'files_detailsTab':'filesTab_files', 
                             'info_detailsTab':'details'}
                             #'graphs_detailsTab':'Tab_graphs'}
            #TODO: change from currentPanel to the string name of the current selected details panel
            #get the currently selected panel 
            current_name = 'details'
            panel_name = 'details'
            for key in tabButtons.keys():
                if name == key:
                    panel_name = tabPanelNames[key]
                if tabButtons[key].isSelected():
                    current_name = tabPanelNames[key]
            panel1 = self.getGuiObj(current_name)
            panel2 = self.getGuiObj(panel_name)
            if panel1 is not None and panel2 is not None and panel1 != panel2:
                if DEBUG:
                    print >>sys.stderr,"standardDetails: <mluc> switching from "+current_name+" to "+panel_name
                self.swapPanel(panel1, panel2)
                
                for key in tabButtons.keys():
                    try:
                        if key == name:
                            tabButtons[key].setSelected(True)
                        else:
                            tabButtons[key].setSelected(False)
                    except:
                        print "tab %s has no button??" % key
                self.currentPanel.SetAutoLayout(1)
                self.currentPanel.Layout()
                self.hSizer.Layout()
        elif self.mode == 'filesMode':
            tabFiles = self.getGuiObj('files_detailsTab')
            tabInfo = self.getGuiObj('info_detailsTab')
            infoPanel = self.getGuiObj('details')
 #            sizer = infoPanel.GetContainingSizer()
            filesPanel = self.getGuiObj('filesTab_files')
            
            if name == 'files_detailsTab' and not tabFiles.isSelected():
                tabFiles.setSelected(True)
                tabInfo.setSelected(False)
                self.swapPanel( infoPanel, filesPanel)#, sizer, 3)
                
            elif name == 'info_detailsTab' and not tabInfo.isSelected():
                tabFiles.setSelected(False)
                tabInfo.setSelected(True)
                self.swapPanel( filesPanel, infoPanel)#, sizer, 3)
            else:
                if DEBUG:
                    print >> sys.stderr,'standardDetails: %s: Unknown tab %s' % (self.mode,name)
                return
#                relayout the details panel to accomodate the new panel
           
            
            self.currentPanel.SetAutoLayout(1)
            self.currentPanel.Layout()
            self.hSizer.Layout()    

        elif self.mode in ["personsMode","friendsMode"]:
            tabAdvanced = self.getGuiObj('advanced_detailsTab')
            tabInfo = self.getGuiObj('info_detailsTab')
            infoPanel = self.getGuiObj('detailsC')
            advancedPanel = self.getGuiObj('personsTab_advanced')
            if name == 'advanced_detailsTab' and not tabAdvanced.isSelected():
                tabAdvanced.setSelected(True)
                tabInfo.setSelected(False)
                self.swapPanel( infoPanel, advancedPanel)
            elif name == 'info_detailsTab' and not tabInfo.isSelected():
                tabAdvanced.setSelected(False)
                tabInfo.setSelected(True)
                self.swapPanel( advancedPanel, infoPanel)
            else:
                if DEBUG:
                    print >>sys.stderr,'standardDetails: %s: Unknown tab %s' % (self.mode,name)
                return
#            print "<mluc> advanced tab has label:",tabAdvanced.GetLabel()

            #relayout the details panel to accomodate the new panel
            self.currentPanel.SetAutoLayout(1)
            self.currentPanel.Layout()
            self.hSizer.Layout()

        elif self.mode == "profileMode":
#            print "<mluc> try to switch to",name
            if name.startswith("bgPanel"):
                name = "profileDetails"+name[7:]
#            if name == "profileDetails_Overall":
#                name = 'panel'
#            print "<mluc> current panel is:",self.item
#            if self.item is None:
#                self.item = 'panel'
            panel1 = self.currentPanel #getGuiObj(self.item)
            panel2 = self.getGuiObj(name)
            if panel1 is not None and panel2 is not None and panel1 != panel2:
#===============================================================================
#                print "<mluc> switch from %s[%s] to %s[%s]" % (panel1.GetName(), panel1.GetParent().GetName(), panel2.GetName(), panel2.GetParent().GetName())
#                if isinstance(panel1,tribler_topButton):
#                    print "<mluc> set unselected for",panel1.GetName()
#                    panel1.setSelected(False)
#                else:
#                    print "<mluc> panel1 ",panel1.GetName()," is of type ",panel1.__class__.__name__
#                if panel2.__class__.__name__.endswith("tribler_topButton"):
#                    print "<mluc> set selected for",panel2.GetName()
#                    panel2.setSelected(True)
#                else:
#                    print "<mluc> panel2 ",panel2.GetName()," is of type ",panel2.__class__.__name__
#===============================================================================
                self.swapPanel(panel1, panel2)
                #each time the panel changes, update the 'panel' reference in data list
                self.data[self.mode]['panel'] = panel2
                #actually, update the currentPanel reference
                self.currentPanel = panel2
#                self.item = name
#            else:
#                print "<mluc> can't switch, one of the panel is None or the same panel"
#                self.currentPanel.Layout()
#                self.currentPanel.SetAutoLayout(1)
#                self.hSizer.Layout()
                if DEBUG:
                    print >>sys.stderr,"standardDetails: <mluc> switch from %s[%s] to %s[%s]" % (panel1.GetName(), panel1.GetParent().GetName(), panel2.GetName(), panel2.GetParent().GetName())
        else:
            if DEBUG:
                print >>sys.stderr,'standardDetails: Tab (%s) for this mode (%s) not yet implemented' % (name,self.mode)
            return
        
        self.setData(self.item)
        self.refreshStandardDetailsHeight()

            
    def swapPanel(self, oldpanel, newpanel, sizer=None, index=-1):
        """replaces in a sizer a panel with another one to simulate tabs"""
        if sizer is None:
            sizer = oldpanel.GetContainingSizer()
            if not sizer:
                return #could not swap
        #if index not given, use sizer's own replace method
        if index == -1:
            index = 0
            for panel in sizer.GetChildren():
                if panel.GetWindow() == oldpanel:
                    break
                index = index + 1
            if index == len(sizer.GetChildren()):
                return #error: index not found so nothing to change
#            sizerItem = sizer.Replace(oldpanel, newpanel)
#            print "found index is:",index,"number of children in sizer:",len(sizer.GetChildren())
        # remove info tab panel
        sizer.Detach(oldpanel)
        oldpanel.Hide()
        #print >>sys.stderr,"standardDetails: <mluc> found sizer equal to hSizer?",(sizer==self.hSizer)
        # add files tab panel
        newpanel.SetAutoLayout(1)
        newpanel.Layout()
        if not newpanel.IsShown():
            newpanel.Show()
        sizer.Insert(index, newpanel, 0, wx.ALL|wx.EXPAND, 0)
        sizer.Layout()
        
    def getAlternativeTabPanel(self, name, parent=None):
        "Load a tabPanel that was not loaded as default"
        panel = self.getGuiObj(name)
        if panel:
            return panel
        else:
            # generate new panel
            xrcResource = os.path.join(self.guiUtility.vwxGUI_path, name+'.xrc')
            if os.path.exists(xrcResource):
                panelName = name
                if parent is None:
                    parent = self.currentPanel
                panel = self.loadXRCPanel(xrcResource, panelName, parent=parent)
            if panel is not None and self.tabElements.has_key(name):
                for element in self.tabElements[name]:
                    xrcElement = xrc.XRCCTRL(panel, element)
                    if not xrcElement:
                        if DEBUG:
                            print >>sys.stderr,'standardDetails: Error: Could not identify xrc element: %s for mode %s' % (element, self.mode)
                        pass
                    self.data[self.mode][name+'_'+element] = xrcElement
                            
            self.data[self.mode][name] = panel
            
            return panel
        
    def mouseAction(self, event):
        """ Arno: apparently not used, see GUIUtility.buttonClicked() """ 
        if DEBUG:
            print >>sys.stderr,'standardDetails: mouseAction'
        
        obj = event.GetEventObject()
        #print obj
        
        if not self.data:
            return
        if obj == self.downloadButton:
            self.download(self.data)
            # --tb--
#        if obj == self.optionsButtonLibrary:
#            # zelfde menu als rechterMuisKnop
#            print "optionsButton"
#            self.rightMouseAction(event)
        elif obj == self.refreshButton: 
            #and self.refreshButton.isEnabled():
            if DEBUG:
                print >>sys.stderr,"standardDetails: refresh seeders and leechers"
            #self.swarmText.SetLabel(self.utility.lang.get('refreshing')+'...')
            #self.swarmText.Refresh()
            
            self.refresh(self.data)
            
    def rightMouseButton(self, event):
        if DEBUG:
            print >>sys.stderr,'standardDetails: --tb-- keydown function(2)'  
        menu = self.guiUtility.OnRightMouseAction(event)
        if menu is not None:
            self.PopupMenu(menu, (-1,-1))   

            
    def refresh(self, torrent):
        if DEBUG:
            print >>sys.stderr,'standardDetails: refresh ' + repr(torrent.get('name', 'no_name'))
        check = TorrentChecking(torrent['infohash'])
        check.start()
        
 
#    def isEnabled(self):
#        return self.enabled

    def download(self, torrent = None, dest = None, secret = False, force = False):
        if torrent is None:
            torrent = self.item
            
        if (torrent is None or torrent.get('myDownloadHistory')) and not force:
            return
            
        #print "**** standdetail: download", `torrent`
            
        if torrent.get('web2'):
            if DEBUG:
                print >>sys.stderr,"standardDetails: Playing WEB2 video: " + torrent['url']
            #self.videoplayer.parentwindow.swapin_videopanel(torrent['url'])
            self.videoplayer.play_url(torrent['url'])
            return True

        if 'query_permid' in torrent:
            if DEBUG:
                print >>sys.stderr,"standardDetails: User selected query result for download"
            try:
                self.utility.session.download_torrentfile_from_peer(torrent['query_permid'],torrent['infohash'],self.sesscb_got_requested_torrent)
            except:
                print_exc()
                print >> sys.stderr, torrent, torrent.keys()
            return True

        torrent_dir = self.utility.session.get_torrent_collecting_dir()
        print >> sys.stderr, 'got torrent to download', 'torrent_file_name' in torrent, torrent
        if 'torrent_file_name' not in torrent:
            filename = get_filename(torrent['infohash']) 
        torrent_filename = os.path.join(torrent_dir, torrent['torrent_file_name'])

        if torrent.get('name'):
            name = torrent['name']
        else:
            name = showInfoHash(torrent['infohash'])
        #start_download = self.utility.lang.get('start_downloading')
        #str = name + "?"
        
        
        if os.path.isfile(torrent_filename):
            
            # Api download
            d = self.utility.frame.startDownload(torrent_filename,destdir=dest)
            if d:
                if secret:
                    self.torrent_db.setSecret(torrent['infohash'], secret)

                if DEBUG:
                    print >>sys.stderr,'standardDetails: download started'
                # save start download time.
                #torrent['download_started'] = time()
                #torrent['progress'] = 0.0
                self.setBelongsToMyDowloadHistory(torrent, True)
                return True        
            else:
                return False
        else:
        
            # Torrent not found            
            str = self.utility.lang.get('delete_torrent') % name
            dlg = wx.MessageDialog(self, str, self.utility.lang.get('delete_dead_torrent'), 
                                wx.YES_NO|wx.NO_DEFAULT|wx.ICON_INFORMATION)
            result = dlg.ShowModal()
            dlg.Destroy()
            if result == wx.ID_YES:
                infohash = torrent['infohash']
                self.torrent_db.deleteTorrent(infohash, delete_file=True, commit = True)
                
                return True
            else:
                return False

    def sesscb_got_requested_torrent(self,infohash,metadata,filename):
        """ The torrent file requested from another peer came in.
        @param infohash The infohash of the torrent file.
        @param metadata The contents of the torrent file (still bencoded)
        """
        # Called by SessionCallback thread
        print >>sys.stderr,"standardDetails: sesscb_got_requested_torrent:",`infohash`
        
        # ARNOCOMMENT: h4x0r this
        torrent = {}
        torrent['torrent_file_name'] = filename
        torrent['infohash'] = infohash
        
        wx.CallAfter(self.download,torrent)


    def setBelongsToMyDowloadHistory(self,torrent, b):
        """Set a certain new torrent to be in the download history or not
        Should not be changed by updateTorrent calls"""

        # DB registration and buddycast notification is done in LaunchManyCore.add()
        # Currently no removal function.
        torrent['myDownloadHistory'] = True


    def setTorrentThumb(self, mode, torrent, thumbPanel):
        
        if not thumbPanel:
            return 
        
        thumbPanel.setBackground(wx.BLACK)
        if mode in  ['filesMode', 'libraryMode']:
            self.getThumbnailLarge(torrent,thumbPanel)
        elif mode in ['personsMode', 'friendMode']:
            # get thumbimage of person
            if False:
                pass
            else:
                default = self.iconsManager.get_default('personsMode','DEFAULT_THUMB')
                thumbPanel.setBitmap(default)
                
    def addAsFriend(self):
        # add the current user selected in details panel as a friend
        if self.mode in ["personsMode","friendsMode"]:
            peer_data = self.item
            if peer_data is not None and peer_data.get('permid'):
               self.friend_db.toggleFriend(peer_data['permid'])


    def refreshTorrentStats(self,dslist):
        """ Called by GUI thread """
        nactive = 0
        
        tl = []
        totaldlspeed = 0.0
        totalulspeed = 0.0
        for ds in dslist:
            d = ds.get_download()
            progress = ds.get_progress()
            
            if progress < 1.0:
                tl.append([progress,d])
            
            totaldlspeed += ds.get_current_speed(DOWNLOAD)
            totalulspeed += ds.get_current_speed(UPLOAD)
            
            status = ds.get_status()
            if status != DLSTATUS_STOPPED and status != DLSTATUS_STOPPED_ON_ERROR:
                nactive += 1

            if DEBUG:
                print >>sys.stderr,"standardDetails: stats:",`d.get_def().get_name()`,progress,status
            


        # Reverse sort on percentage done, get top 4 
        tl.sort(revtcmp)
        ml = min(len(tl),4)
        newtl = tl[:ml]
        
        for i in range(4):
            if i < ml:
                (progress,d) = newtl[i]
                progresstxt = progress2txt(progress)
                file = d.get_def().get_name_as_unicode()
            else:
                progresstxt = ''
                file = ''
            tname = 'download'+str(i+1)
            pname = 'percent'+str(i+1)
            tlabel = self.data['status'][tname]
            plabel = self.data['status'][pname]
            #print "Setting",pname,"to",progresstxt
            tlabel.SetLabel(file[:45])
            plabel.SetLabel(progresstxt[:45])
        statdlpanel = self.data['status']['panel']
        
        self.refreshTorrentTotalStats(nactive,totaldlspeed,totalulspeed)
        
        statdlpanel.Refresh()


    def refreshTorrentTotalStats(self,nactive,totaldlspeed,totalulspeed):
        """ Called by GUI thread """
        leftlabel = self.data['status']['st28c']
        rightlabel = self.data['status']['downSpeed']
        rightlabel2 = self.data['status']['upSpeed']
        
        lefttext = self.utility.lang.get('downloading')+' ('+str(nactive)+')'
        righttxt = str(int(totaldlspeed))+' KB/s'
        righttxt2 = str(int(totalulspeed))+' KB/s'
        leftlabel.SetLabel(lefttext)
        rightlabel.SetLabel(righttxt)
        rightlabel2.SetLabel(righttxt2)

    def updateLastCheck(self, event=None):
        #print 'updateLastCheck'
        if self.item and self.item.has_key('last_check_time'):
            last_time = self.item.get('last_check_time')
            if last_time and type(last_time) == int:
                self.getGuiObj('refresh').SetToolTipString('%s: %s' % (self.utility.lang.get('last_checked'), friendly_time(last_time)))
        event.Skip()
        
    """
    def subscrNeedsGUIUpdate(self,todayl,yesterdayl):
        update = True
        if len(todayl) > 0:
            if self.subscrDataCopy_today_top is not None and self.subscrDataCopy_today_top == todayl[0]:
               update = False
            self.subscrDataCopy_today_top = todayl[0]
            
        if len(yesterdayl) > 0:
            if self.subscrDataCopy_yday_top is not None and self.subscrDataCopy_yday_top == yesterdayl[0]:
               update = False
            self.subscrDataCopy_yday_top = yesterdayl[0]
        return update
    """
            
    def getThumbnailLarge(self,torrent,thumbPanel):
        readable = torrent.get('metadata',{}).get('ThumbReadable')
        if readable == False:
            default = self.iconsManager.getCategoryIcon('filesMode',torrent.get('category'), 'large')
            thumbPanel.setBitmap(default)
            return

        if 'preview' in torrent:
            thumbnailString = torrent['preview']
        else:
            # Arno: Read big image on demand
            torrent_dir = self.utility.session.get_torrent_collecting_dir()
            torrent_filename = os.path.join(torrent_dir, torrent['torrent_file_name'])
            metadata = loadAzureusMetadataFromTorrent(torrent_filename)
            if metadata:
                thumbnailString = metadata.get('Thumbnail')
            else:
                thumbnailString = None

        if 'metadata' not in torrent:
            torrent['metadata'] = {}
            
        if thumbnailString:
            img = createThumbImage(thumbnailString)

            #print 'Found thumbnail: %s' % thumbnailString
            iw, ih = img.GetSize()
            w, h = thumbPanel.GetSize()
            if (iw/float(ih)) > (w/float(h)):
                nw = w
                nh = int(ih * w/float(iw))
            else:
                nh = h
                nw = int(iw * h/float(ih))
            if nw != iw or nh != ih:
                #print 'Rescale from (%d, %d) to (%d, %d)' % (iw, ih, nw, nh)
                try:
                    # if wx >= 2.7, use Bicubic scaling
                    img.Rescale(nw, nh, quality = wx.IMAGE_QUALITY_HIGH)
                except:
                    img.Rescale(nw, nh)
            bmp = wx.BitmapFromImage(img)
             
            thumbPanel.setBitmap(bmp)
            torrent['metadata']['ThumbReadable'] = True
        else:
            #print 'Torrent: %s' % torrent
            torrent['metadata']['ThumbReadable'] = False
            
            #print "****** torrent", torrent
            
            default = self.iconsManager.getCategoryIcon('filesMode',torrent.get('category','all'), 'large')
            thumbPanel.setBitmap(default)

    def refreshStandardDetailsHeight(self, panel = None):
        if not panel:
            panel = self.currentPanel
        margin = 6
        if self.data.get('status',{}).get('panel'):
            statusPanelHeight = self.data['status']['panel'].GetSize()[1]
        else:
            statusPanelHeight = 0
        
        newHeight = panel.GetSize()[1] + statusPanelHeight + margin
        size = (300,newHeight)
        self.SetSize(size)
        self.SetMinSize(size)
        self.SetMaxSize(size)
        self.GetContainingSizer().Layout()
        # Resize scrollWindow to make scrollbars update to new windowsize
        self.guiUtility.scrollWindow.FitInside()
        self.Refresh()
        
        if DEBUG:
            print 'StandardDetails: setting size of stand.details to: %s' % str(size)
            
    def topNListText(self, tab):
        if not self.bartercastdb:
            self.bartercastdb = BarterCastDBHandler.getInstance()
        
        top_stats = self.bartercastdb.getTopNPeers(10)
        top = top_stats['top']
        #total_up = top_stats['total_up']
        #total_down = top_stats['total_down']
        tribler_up = top_stats['tribler_up']
        tribler_down = top_stats['tribler_down']
        
        rank = 1
        topText = ''
        for permid, up, down in top:
            
            # up and down are integers in KB in the database
            # (for overhead limitation)
            amount_str_up = self.utility.size_format(up)
            amount_str_down = self.utility.size_format(down)

            name = self.bartercastdb.getName(permid)

            topText += '%d. %s%s     up: %s (down: %s)%s%s' % (rank, name, os.linesep, 
                                                     amount_str_up, amount_str_down, os.linesep, os.linesep)
            rank+=1
        
        self.getGuiObj('descriptionField0', tab = tab).SetLabel(topText)
        self.getGuiObj('descriptionField0', tab = tab).Refresh()
        self.getGuiObj('downloadedNumberT', tab = tab).SetLabel(self.utility.size_format(tribler_down))
        self.getGuiObj('uploadedNumberT', tab = tab).SetLabel(self.utility.size_format(tribler_up))


    def updateCallback(self, item):
        "Update callback handling for this item"
        session = self.guiUtility.utility.session
        session.remove_observer(self.db_callback)
        if item is None:
            return
        if self.mode in ['filesMode', 'libraryMode']:
            session.add_observer(self.db_callback, NTFY_TORRENTS, [NTFY_UPDATE, NTFY_DELETE], item['infohash'])
        elif self.mode in ['personsMode', 'friendsMode']:
            session.add_observer(self.db_callback, NTFY_PEERS, [NTFY_UPDATE, NTFY_DELETE], item['permid'])
        elif self.mode == 'subscriptionsMode':
            pass
        elif self.mode == 'profileMode':
            pass
        
    def db_callback(self,subject,changeType,objectID,*args):
        # called by threadpool thread
        print >> sys.stderr, 'stdDetails: db_callback: %s %s %s %s' % (subject, changeType, `objectID`, args)
        db_handler = self.guiUtility.utility.session.open_dbhandler(subject)
        if subject == NTFY_PEERS:
            newitem = db_handler.getPeer(objectID)
        elif subject in (NTFY_TORRENTS):
            newitem = db_handler.getTorrent(objectID)
            
        wx.CallAfter(self.setData, newitem)
        
def revtcmp(a,b):
    if a[0] < b[0]:
        return 1
    elif a[0] == b[0]:
        return 0
    else:
        return -1

def reverse_torrent_insertime_cmp(a,b):
    if a['insert_time'] < b['insert_time']:
        return 1
    elif a['insert_time'] == b['insert_time']:
        return 0
    else:
        return -1
    
def getShortTrackerFormat(n):
    try:
        t = urlparse.urlsplit(n)
        short = t[1]
        idx = t[1].find(':')
        if idx == -1:
            short = t[1]
        else:
            short = t[1][:idx]
        if sys.platform == 'linux2':
            short = short[:27]
    except:
        short = n[:27]
    return ' '+short


def progress2txt(progress):
    # Truncate the progress value rather than round down
    # (will show 99.9% for incomplete torrents rather than 100.0%)
    progress = int(progress * 1000)/10.0
    
    return ('%.1f' % progress) + "%"
