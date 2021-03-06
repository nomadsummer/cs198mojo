import wx, os, sys
from traceback import print_exc
from Tribler.Main.vwxGUI.GuiUtility import GUIUtility

DEBUG = False

class tribler_topButton(wx.Panel):
    """
    Button that changes the image shown if you move your mouse over it.
    It redraws the background of the parent Panel, if this is an imagepanel with
    a variable self.bitmap.
    """

    def __init__(self, *args, **kw):
#        print "<mluc> tribler_topButton in init"
        self.initDone = False
        self.enabled = True
        if len(args) == 0: 
            self.backgroundColor = wx.Colour(102,102,102) 
            pre = wx.PrePanel() 
            # the Create step is done by XRC. 
            self.PostCreate(pre) 
            self.Bind(wx.EVT_WINDOW_CREATE, self.OnCreate) 
        else:
            self.backgroundColor = wx.Colour(102,102,102) 
            wx.Panel.__init__(self, *args, **kw) 
            self._PostInit()     
        
    def OnCreate(self, event):
#        print "<mluc> tribler_topButton in OnCreate"
        self.Unbind(wx.EVT_WINDOW_CREATE)
        wx.CallAfter(self._PostInit)
        event.Skip()
        return True
    
    def _PostInit(self):
#        print "<mluc> tribler_topButton in _PostInit"
        # Do all init here
        self.guiUtility = GUIUtility.getInstance()
        self.utility = self.guiUtility.utility
        self.Bind(wx.EVT_MOUSE_EVENTS, self.mouseAction)
        self.Bind(wx.EVT_LEFT_UP, self.ClickedButton)
        self.selected = False
        self.tooltip = None
        self.old_bitmaps = None #bitmaps that were initially loaded on the button with searchBitmaps function, and now have been changed to some provisory ones using switchTo
        self.searchBitmaps()
        self.createBackgroundImage()
        
        #<mluc> on mac, the button doesn't get a size
        #if self.bitmaps[0] and self.GetSize()==(0,0):
        if self.bitmaps[0]:
            self.SetSize(self.bitmaps[0].GetSize())
#        print >> sys.stderr, self.Name
#        print >> sys.stderr, 'size'
#        print >> sys.stderr, self.Size
        
        
        self.initDone = True
        self.Refresh(True)
        self.Update()
        
        
    def searchBitmaps(self):
        self.bitmaps = [None, None]
        self.parentBitmap = None
        self.mouseOver = False
                
        # get the image directory
        self.imagedir = os.path.join(self.guiUtility.vwxGUI_path, 'images')
       
        if not os.path.isdir(self.imagedir):
            print 'Error: no image directory found in %s' % self.imagedir
            return
        
        # find a file with same name as this panel
        self.bitmapPath = [os.path.join(self.imagedir, self.GetName()+'.png'), 
                        os.path.join(self.imagedir, self.GetName()+'_clicked.png')]
        
        i = 0
        for img in self.bitmapPath:
            if os.path.isfile(img):
                self.bitmaps[i] = wx.Bitmap(img, wx.BITMAP_TYPE_ANY)
                i+=1
            elif DEBUG:
                print 'Could not find image: %s' % img
         
           
    def setBitmaps(self, normalBitmap, selectedBitmap=None):
        # This function does not protect you as switch* do.
        self.bitmaps=[normalBitmap,selectedBitmap]
        self.Refresh()
           
    def switchTo(self, normalBitmap, selectedBitmap=None):
        if self.old_bitmaps is not None:
            if DEBUG:
                print "tribler_TopButton: First should switchBack..."
        else:
            #save the initial bitmaps
            self.old_bitmaps = self.bitmaps
        self.bitmaps=[normalBitmap,selectedBitmap]
        #should Refresh?
        self.Refresh()
    
    def switchBack(self):
        if self.old_bitmaps!=None:
            self.bitmaps = self.old_bitmaps
            self.old_bitmaps=None
            self.Refresh()
        else:
            if DEBUG:
                print "Nothing to switch back to..."
        
        
    def createBackgroundImage(self):
        if self.bitmaps[0]:
            wx.EVT_PAINT(self, self.OnPaint)
            self.Bind(wx.EVT_ERASE_BACKGROUND, self.OnErase)
                
    
    def OnErase(self, event):
        pass
        #event.Skip()
        
    def setSelected(self, sel):
        self.selected = sel
        self.Refresh()
        
    def isSelected(self):
        return self.selected
        
    def mouseAction(self, event):
        event.Skip()
        if event.Entering():
            #print 'enter' 
            self.mouseOver = True
            self.Refresh()
        elif event.Leaving():
            self.mouseOver = False
            #print 'leave'
            self.Refresh()


    def ClickedButton(self, event):
        event.Skip()
        if self.enabled:
            self.guiUtility.buttonClicked(event)
                
    def getParentBitmap(self):
        try:
            parent = self.GetParent()
            bitmap = parent.bitmap
            #print bitmap
        except:
            return None
        
        if bitmap:
            location = self.GetPosition()
            #location[0] -= parent.GetPosition()[0]
            #location[1] -= parent.GetPosition()[1]
            #if DEBUG:
            #    print '(button %s) Mypos: %s, Parentpos: %s' % (self.GetName(), self.GetPosition(), parent.GetPosition())
            rect = [location[0], location[1], self.GetClientSize()[0], self.GetClientSize()[1]]
            #if DEBUG:
            #    print '(button %s) Slicing rect(%d,%d) size(%s) from parent image size(%s)' % (self.GetName(), location[0], location[1], str(self.GetClientSize()), str(bitmap.GetSize()))
            bitmap = self.getBitmapSlice(bitmap, rect)
            return bitmap
        else:
            return None
    
    def joinImage(self, im1,im2,offsetx=0,offsety=0):
        "Draw im2 on im1"
        stopx = im2.GetWidth()
        if stopx > (im1.GetWidth()-offsetx):
            stopx = im1.GetWidth()-offsetx
        stopy = im2.GetHeight()
        if stopy > (im1.GetHeight()-offsety):
            stopy = im1.GetHeight()-offsety
        if stopx>0 and stopy>0:
            for x in range(0,stopx):
                for y in range(0,stopy):
                    rgb2 = (im2.GetRed(x,y),im2.GetGreen(x,y),im2.GetBlue(x,y))
                    if rgb2 !=(255,0,255):
                        im1.SetRGB(x+offsetx,y+offsety,rgb2[0],rgb2[1],rgb2[2])
        return im1
 
    def getBitmapSlice(self, bitmap, rect):
        try:
            #print rect
            bitmapSize = bitmap.GetSize()
            rect[0] %= bitmapSize[0]
            rect[1] %= bitmapSize[1]
            rects = [rect]
            if rect[0]+rect[2] > bitmapSize[0]:
                rect1 = (rect[0], rect[1], bitmapSize[0]-rect[0], rect[3])
                rect2 = (0, rect[1], rect[0]+rect[2] - bitmapSize[0], rect[3])
                rects = [rect1, rect2]
            if rect[1]+ rect[3] > bitmapSize[1]:
                rects2 = []
                for r in rects:
                    r1 = (r[0], r[1], r[2], bitmapSize[1] - r[3])
                    r2 = (r[0], 0, r[2], r[1]+r[3] - bitmapSize[1])
                    rects2.append(r1)
                    rects2.append(r2)
                rects = rects2
            images = []
            if len(rects) > 1:
                if DEBUG:
                    print "(button %s) Result: %s" % (self.GetName(), rects)
                image = wx.EmptyImage(rect[2], rect[3])
                for r in rects:    
                    rect = wx.Rect(r[0], r[1], r[2], r[3])
                    if DEBUG:
                        print '(button %s) Trying to get rect: %s from bitmap: %s' % (self.GetName(), rect, bitmap.GetSize())
                    subBitmap = bitmap.GetSubBitmap(rect)
                    subImage = subBitmap.ConvertToImage()
                    if len(rects) == 2:
                        if r == rects[0]:
                            place = (0,0)
                        elif r == rects[1]:
                            place = (rects[0][2], 0)
                    elif len(rects) == 4:
                        if r == rects[0]:
                            place = (0,0)
                        elif r == rects[1]:
                            place = (0, rects[0][3])
                        elif r == rects[2]:
                            place = (rects[0][2],0)
                        elif r == rects[3]:
                            place = (rects[0][2], rects[0][3])
                    if DEBUG:
                        print "(button %s) Place subbitmap: %s" % (self.GetName(), str(place))
                    self.joinImage(image, subImage, place[0], place[1])
                if DEBUG:
                    print '(button %s) Result img size: %s' % (self.GetName(), str(image.GetSize()))
                return image.ConvertToBitmap()
            else:
                return bitmap.GetSubBitmap(wx.Rect(rect[0], rect[1], rect[2], rect[3]))
        except:
            if DEBUG:
                print_exc()
            return None
                                            
    def setEnabled(self, e):
        self.enabled = e
        if not e:
            self.SetToolTipString('')
#        else:
#            if self.tooltip:
#                self.SetToolTipString(self.tooltip)
        self.Refresh()
        
    def isEnabled(self):
        return self.enabled
    
    def setBackground(self, wxColor):
        self.backgroundColor = wxColor
        self.Refresh()
        
    def OnPaint(self, evt):
        dc = wx.BufferedPaintDC(self)
        dc.SetBackground(wx.Brush(self.backgroundColor))
        dc.Clear()
        
        if self.parentBitmap:
            dc.DrawBitmap(self.parentBitmap, 0,0, True)
        else:
            self.parentBitmap = self.getParentBitmap()
            if self.parentBitmap:
                dc.DrawBitmap(self.parentBitmap, 0,0, True)
        
        if not self.enabled:
            return
        
        if self.bitmaps[0]:
            dc.DrawBitmap(self.bitmaps[0], 0,0, True)
        if (self.mouseOver or self.selected) and self.bitmaps[1]:
            dc.DrawBitmap(self.bitmaps[1], 0,0, True)
        

class SwitchButton(tribler_topButton):
        
    def searchBitmaps(self):
        self.toggled = False
        self.allBitmaps = [None, None, None, None]
        self.parentBitmap = None
        self.mouseOver = False
                
        # get the image directory
        abcpath = self.utility.getPath()
        self.imagedir = os.path.join(self.guiUtility.vwxGUI_path, 'images')
        if not os.path.isdir(self.imagedir):
            olddir = self.imagedir
            # Started app.py in vwxDir?
            self.imagedir = os.path.join(abcpath, 'images')
            
        if not os.path.isdir(self.imagedir):
            print >> sys.stderr, 'Error: no image directory found in %s and %s' % (olddir, self.imagedir)
            return
        
        # find a file with same name as this panel
        self.bitmapPath = [os.path.join(self.imagedir, self.GetName()+'.png'), 
                        os.path.join(self.imagedir, self.GetName()+'_clicked.png'),
                        os.path.join(self.imagedir, self.GetName()+'Enabled.png'), 
                        os.path.join(self.imagedir, self.GetName()+'Enabled_clicked.png')
                        ]
        
        i = 0
        for img in self.bitmapPath:
            if os.path.isfile(img):
                self.allBitmaps[i] = wx.Bitmap(img, wx.BITMAP_TYPE_ANY)
                i+=1
            elif DEBUG:
                print >> sys.stderr, 'Could not find image: %s' % img
                

        if self.toggled:
            self.bitmaps = self.allBitmaps[2:]
        else:
            self.bitmaps = self.allBitmaps[:2]
        #print >> sys.stderr, 'Switchbutton (%s) bitmaps: %s' % (self.Name, self.allBitmaps)
                
    def setToggled(self, b = None, tooltip = { "enabled": "", "disabled": ""}):
        if b is None:
            b = not self.toggled
        self.toggled = b

        if not self.initDone:
            return

        if b:
            self.bitmaps=self.allBitmaps[2:]
            if self.enabled:
                self.SetToolTipString(tooltip["enabled"])
        else:
            self.bitmaps=self.allBitmaps[:2]
            if self.enabled:
                self.SetToolTipString(tooltip["disabled"])
            
        #print >> sys.stderr, 'Bitmaps is now: %s' % self.bitmaps
        #should Refresh?
        self.Refresh()
        
    def isToggled(self):
        return self.toggled
    

class PlayerSwitchButton(tribler_topButton):
        
    def __init__(self, imagedir, filename):
        self.initDone = False
        self.enabled = True
        self.backgroundColor = wx.Colour(102,102,102) 
        wx.Panel.__init__(self, *args, **kw) 
        self.selected = False
        self.tooltip = None
        self.old_bitmaps = None #bitmaps that were initially loaded on the button with searchBitmaps function, and now have been changed to some provisory ones using switchTo
        self.searchBitmaps()
        self.createBackgroundImage()
        self.imagedir = path
        self.filename = filename
        
        #<mluc> on mac, the button doesn't get a size
        #if self.bitmaps[0] and self.GetSize()==(0,0):
        if self.bitmaps[0]:
            self.SetSize(self.bitmaps[0].GetSize())
#        print self.Name
#        print 'size'
#        print self.Size
        
        
        self.initDone = True
        self.Refresh(True)
        self.Update()
        
    def searchBitmaps(self):
        self.toggled = False
        self.allBitmaps = [None, None, None, None]
        self.parentBitmap = None
        self.mouseOver = False
                
                    
        if not os.path.isdir(self.imagedir):
            print 'Error: no image directory found in %s and %s' % (olddir, self.imagedir)
            return
        
        # find a file with same name as this panel
        self.bitmapPath = [os.path.join(self.imagedir, self.filename+'.png'), 
                        os.path.join(self.imagedir, self.filename+'_clicked.png'),
                        os.path.join(self.imagedir, self.filename+'Enabled.png'), 
                        os.path.join(self.imagedir, self.filename+'Enabled_clicked.png')
                        ]
        
        i = 0
        for img in self.bitmapPath:
            if os.path.isfile(img):
                self.allBitmaps[i] = wx.Bitmap(img, wx.BITMAP_TYPE_ANY)
                i+=1
            elif DEBUG:
                print 'Could not find image: %s' % img
                

        if self.toggled:
            self.bitmaps = self.allBitmaps[2:]
        else:
            self.bitmaps = self.allBitmaps[:2]
                
    def setToggled(self, b, tooltip = { "enabled": "", "disabled": ""}):
        self.toggled = b

        if not self.initDone:
            return

        if b:
            self.bitmaps=self.allBitmaps[2:]
            if self.enabled:
                self.SetToolTipString(tooltip["enabled"])
        else:
            self.bitmaps=self.allBitmaps[:2]
            if self.enabled:
                self.SetToolTipString(tooltip["disabled"])
            
        #print 'Bitmaps is now: %s' % self.bitmaps
        #should Refresh?
        self.Refresh()
        
    def isToggled(self):
        return self.toggled
    
