import os
import sys
import unittest
import tempfile
from traceback import print_exc
import thread, threading

if os.path.exists('test_bsddb2sqlite.py'):
    BASE_DIR = '..'
    sys.path.insert(1, os.path.abspath('..'))
elif os.path.exists('LICENSE.txt'):
    BASE_DIR = '.'
    
from Core.CacheDB.bsddb2sqlite import Bsddb2Sqlite
from Core.CacheDB.sqlitecachedb import SQLiteCacheDB
    
CREATE_SQL_FILE = os.path.join(BASE_DIR, 'tribler_sdb_v1.sql')
assert os.path.isfile(CREATE_SQL_FILE)
BSDDB_DIR = os.path.join(BASE_DIR, 'Test/extend_db_dir/bsddb')
assert os.path.isdir(BSDDB_DIR), "Please download bsddb from http://www.st.ewi.tudelft.nl/~jyang/donotremove/bsddb.zip and unzip it to %s"%os.path.abspath(BSDDB_DIR)
DB_FILE_NAME = 'tribler.sdb'
DB_DIR_NAME = None

SQLiteCacheDB.DEBUG = False

class TestSqliteCacheDB(unittest.TestCase):
    
    def setUp(self):
        self.tmp_dir = tempfile.gettempdir() 
        self.db_path = tempfile.mktemp()
        self.db_name = os.path.split(self.db_path)[1]
        
    def tearDown(self):
        if os.path.exists(self.db_path):
            os.remove(self.db_path)
    
    def test_convert_db(self):
        try:
            bsddb2sqlite = Bsddb2Sqlite(BSDDB_DIR, self.db_path, CREATE_SQL_FILE)
            bsddb2sqlite.run(torrent_dir=BSDDB_DIR)
            
            sdb = SQLiteCacheDB.getInstance()
            sdb.openDB(self.db_path, 0)
            nconnpeers = sdb.fetchone('select count(*) from Peer where connected_times>0;')
            assert nconnpeers == 1466
            
            ntorrents = sdb.fetchone('select count(*) from torrent;')
            assert ntorrents == 42934
            
            ntorrents = sdb.fetchone('select count(torrent_file_name) from torrent;')
            assert ntorrents == 4848
            
            nprefs = sdb.fetchone('select count(*) from Preference;')
            assert nprefs == 60634
            
            nmyprefs = sdb.fetchone('select count(*) from MyPreference')
            assert nmyprefs == 12
            
            nfriends = sdb.fetchone('select count(*) from peer where friend==1')
            assert nfriends == 2
            nfriends = sdb.fetchone('select count(*) from Friend')
            assert nfriends == 2
            nsuperpeers = sdb.fetchone('select count(*) from peer where superpeer==1')
            assert nsuperpeers == 8      
            nsuperpeers = sdb.fetchone('select count(*) from SuperPeer')
            assert nsuperpeers == 8      
            
            nbc = sdb.fetchone('select count(*) from BarterCast')
            assert nbc == 584
            
            torrent_dir = sdb.fetchone("select value from MyInfo where entry='torrent_dir'")
            assert torrent_dir == BSDDB_DIR, torrent_dir

            sdb.close()
            
            
        finally:
            bsddb2sqlite.close()
        

def test_suite():
    suite = unittest.TestSuite()
    suite.addTest(unittest.makeSuite(TestSqliteCacheDB))
    
    return suite
        
def main():
    unittest.main(defaultTest='test_suite')

if __name__ == '__main__':
    main()    
    #my()
            