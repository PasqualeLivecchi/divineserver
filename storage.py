# def __init__(self,location=".//db.db"):#     self.location = location#     self.currentsize = linecache.getline(location,1)# def load(self,key):#     data = linecache.getline(self.location,key)#     return data if data else "Key not found"from collections import deque
import os,json,mmap
class Storage:
    def __init__(self , location=".\\db"):
        self.location = os.path.expanduser(location)
        self.loaddb(self.location)

    def loaddb(self, key):
        self.db = json.load(open(self.location, "r")) if os.path.exists(location) else {}
        return True

    def savedb(self):
        try:
            json.dump(self.db , open(self.location, "w+"))
            return True
        except:
            return False

    def save(self, key, value):
        try:
            self.db[str(key)] = value
            self.savedb()
            return True
        except Exception as e:
            print("[X] Error Saving Values to Database : " + str(e))
            return False

    def get(self , key):
        try:
            return self.db[key]
        except KeyError:
            print("No Value Can Be Found for " + str(key))
            return False

    def delete(self , key):
        if not key in self.db:
            return False
        del self.db[key]
        self.dumpdb()
        return True

    def resetdb(self):
        self.db={}
        self.dumpdb()
        return True