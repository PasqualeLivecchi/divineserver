from copy import deepcopy
from pprint import pprint
import inspect


class Parser:
    text = ""
    length = 0
    stack = [0 for i in range(256)]
    frame = 0
    maxindex = -1

    def __init__(self, text, encoding='utf-8'):
        if isinstance(text,str):
            self.text = text
            self.length = len(text)
            self.maxindex = -1
        else:
            raise ValueError("string required")

    def i(self, index=None):
        if index:
            self.stack[self.frame] += index
            # print(f"self.maxindex: {self.maxindex} self.stack[self.frame]: {self.stack[self.frame]}")
            if self.maxindex < self.stack[self.frame]:
                self.maxindex = self.stack[self.frame]
        else:
            return self.stack[self.frame]

    def begin(self):
        # print("parser begin")
        self.frame += 1
        if self.frame == len(self.stack):
            a = range(2*self.frame)
            a = [deepcopy(self.stack[x]) for x in range(self.frame)]
            self.stack = a
        self.stack[self.frame] = self.stack[self.frame-1]
        return self.i()

    def rollback(self):
        # print("parser rollback")
        self.stack[self.frame] = 0 if self.frame == 0 else self.stack[self.frame-1]

    def success(self, t=None):
        # print("parser success")
        if t:
            self.success()
            return t
        else:
            self.frame -= 1
            self.stack[self.frame] = self.stack[self.frame+1]
            return True

    def failure(self, t=None):
        # print("parser failure")
        if t:
            self.failure()
            return t
        else:
            self.frame -= 1
            return False

    def currentindex(self):
        # print("parser currentindex")
        return self.i()

    def highindex(self):
        # print("parser highindex")
        return self.maxindex

    def lastchar(self):
        # print("parser lastchar")
        return self.text[self.i()-1]

    def currentchar(self):
        # print("parser currentchar")
        return self.text[self.i()]

    def endofinput(self):
        # print("parser endofinput")
        return self.i() >= self.length

    def match(self, char=None, string=None):
        # print(f"parser match")
        if char:
            if self.endofinput() or self.text[self.i()] != char:
                return False
            self.i(1)
            return True
        if string:
            # print(f"parser match string:{string}")
            n = len(string)
            if not self.regionmatches(self.i(),string,n):
                return False
            self.i(n)
            return True

    def matchignorecase(self, s):
        # print("parser matchignorecase")
        n = len(s)
        if not self.regionmatches(self.i(),s,n):
            return False
        self.i(n)
        return True

    def anyof(self, s):
        # print("parser anyof")
        if self.endofinput() or s.find(self.text[self.i()]) == -1:
            return False
        self.i(1)
        return True

    def noneof(self, s):
        # print("parser noneof")
        if self.endofinput() or s.find(self.text[self.i()]) != -1:
            return False
        self.i(1)
        return True

    def incharrange(self, clow, chigh):
        # print("parser incharrange")
        if self.endofinput():
            print(f"parser incharrange endofinput:{self.endofinput()}")
            return False
        c = chr(ord(self.text[self.i()]))
        if c < clow or c > chigh:
            # print(f"char:{c}")
            return False
        self.i(1)
        return True

    def anychar(self):
        # print("parser anychar")
        if self.endofinput():
            return False
        self.i(1)
        return True

    def test(self, char=None, string=None):
        # print("parser test c or s or none")
        if char:
            return not self.endofinput() and self.text[self.i():self.i()+1] == char
        if string:
            return self.regionmatches(self.i(),string,len(string))

    def test(self, string):
        # print("parser test")
        return self.regionmatches(self.i(),string,len(string))

    def testignorecase(self, string):
        # print("parser testignorecase")
        return self.regionmatches(self.i(),string,len(string))

    def textfrom(self, start):
        # print("parser textfrom")
        return self.text[start:self.i()]
    
    def regionmatches(self,start,string,numofchar):
        return string[0:numofchar] == self.text[start:start+numofchar]


class ParseException(Exception):
    def __init__(self,parser,msg=""):
        self.text = parser.text
        self.errorindex = parser.currentindex()
        self.highindex = parser.highindex()
        super(ParseException, self).__init__(msg)

    def location(self, index):
        line,i,pos = 0,-1,0
        while True:
            j = self.text.find('\n',i)
            if j == -1 or j >= index:
                break
            i = j
            line += 1
        pos = index - i - 1
        return line,pos

    def lines(self):
        return [l.replace('\r', '\r\n') for l in self.text.split("\n")]

    def __str__(self):
        line, pos, msg, = "",0,super(ParseException, self).__str__()
        if '\n' not in self.text:
            line = self.text
            pos = self.errorindex
            msg += " (position " + str(pos+1) + ")\n"
            print(f"newline not in lines. text:{line} pos:{pos} msg:{msg}")
        else:
            loc = self.location(self.errorindex)
            line = self.lines()[loc[0]]
            pos = loc[1]
            print(f"newline in lines:{self.lines()} loc:{self.location(self.errorindex)} line:{line} pos:{pos}")
            msg += " (line " + str(line+1) + ", pos " + str(pos+1) + ")\n"
        msg += line + "\n"
        for i in range(pos):
            msg += '\t' if line[i] == '\t' else ' '
        msg += "^\n"
        return msg
