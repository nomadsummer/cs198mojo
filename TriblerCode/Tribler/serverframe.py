import wx
import sys

class ServerFrame(wx.Frame):
    def __init__(self, title):
        wx.Frame.__init__(self, None, title=title, pos=(150,150), size=(500,400))
        # self.Bind(wx.EVT_CLOSE, self.OnClose)

        # menuBar = wx.MenuBar()
        # menu = wx.Menu()
        # m_exit = menu.Append(wx.ID_EXIT, "E&xit\tAlt-X", "Close window and exit program.")
        # self.Bind(wx.EVT_MENU, self.OnClose, m_exit)
        # menuBar.Append(menu, "&File")
        # self.SetMenuBar(menuBar)

        # self.statusbar = self.CreateStatusBar()

        panel = wx.Panel(self)
        box = wx.BoxSizer(wx.VERTICAL)

        self.status = wx.StaticText(panel, -1, "Hello World!")
        # self.status.SetFont(wx.Font(14, wx.SWISS, wx.NORMAL, wx.BOLD))
        self.status.SetSize(self.status.GetBestSize())
        box.Add(self.status, 0, wx.ALL, 10)

        # m_close = wx.Button(panel, wx.ID_CLOSE, "Close")
        # m_close.Bind(wx.EVT_BUTTON, self.OnClose)
        # box.Add(m_close, 0, wx.ALL, 10)

        panel.SetSizer(box)
        panel.Layout()
    
    def set_player_status(self,s):
        wx.CallAfter(self.OnSetStatus,s)
        
    def OnSetStatus(self,s):
        self.status.SetLabel(s)
        
    # def OnClose(self):
        # print >>sys.stderr, "End of UI"