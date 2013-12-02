import sys
from collections import defaultdict

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

class ServerApp(wx.App):
    def __init__(self):
        wx.App.__init__(self)
        # print >>sys.stderr, "Connected Starting Video Frame!"
        # print >>sys.stderr, "Connected Starting Video Frame!"
        # print >>sys.stderr, "Connected Starting Video Frame!"
        
    def OnInit(self):
        self.start_video_frame()

    def start_video_frame(self):
        # print >>sys.stderr, "Connected Starting Video Frame!"
        # print >>sys.stderr, "Connected Starting Video Frame!"
        # print >>sys.stderr, "Connected Starting Video Frame!"
        
        # Start video frame
        self.videoFrame = PlayerFrame(self)
        self.Bind(wx.EVT_CLOSE, self.videoFrame.OnCloseWindow)
        self.Bind(wx.EVT_QUERY_END_SESSION, self.videoFrame.OnCloseWindow)
        self.Bind(wx.EVT_END_SESSION, self.videoFrame.OnCloseWindow)
        self.videoFrame.Show(True)

        if self.videoplay is not None:
            self.videoplay.set_parentwindow(self.videoFrame)
        self.said_start_playback = False