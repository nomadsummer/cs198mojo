import sys
from collections import defaultdict

class SingletonType(type):
    #Called when an already created class is called to instantiate a new object.
    def __call__(cls, *args, **kwargs): 
        try:
            # If [__instance] is exists, return that instance.
            return cls.__instance
        except AttributeError:
            # If [__instance] is not exists, instantiate new and set [__instance] to that instance.
            # Then return it
            cls.__instance = super(SingletonType, cls).__call__(*args, **kwargs)
            return cls.__instance
    
class MJLogger(object):
    __metaclass__ = SingletonType
    
    def __init__(self):
        # The Main Class Array
        self.data = defaultdict()
        # self.data = {}
        # self.data['Main'] = []
        # self.data['Main'] = []
        # self.data['Main'].append((1000010, 'Samplex Majority', 'In the Index'))
        # self.data['Main'].append((1000020, 'Majora\s Mask', 'The Library'))
    
    def log(self, logname, data):
        if logname in self.data:
            self.data[logname].append(data)
        else:
            self.data[logname] = [data]
        return

    def debuglog(self, logname):
        if logname in self.data:
            print >>sys.stderr, "[MJ-Log_%s]:" % logname
            for i in range(0, len(self.data[logname])):
                print self.data[logname][i]
        else:
            print >>sys.stderr, '>> No Log of that type found!'
        return

    def update(self, logname, data):
        self.data[logname] = [data]
        return

    def is_existing(self, logname):
        if logname in self.data:
            return True
        else:
            return False

# a = MJLogger()
# b = MJLogger()

# a.log('New', (1, 150))
# a.log('New', ("Samplex", 1235))
# a.debuglog('Main')
# a.debuglog('New')