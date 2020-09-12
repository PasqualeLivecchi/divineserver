from copy import deepcopy


class Parser:
    text = ""
    length = 0
    stack = [256]
    frame = 0
    inthigh = 0

    def __init__(self, text):
        self.text = text
        self.length = len(text)

    def i(self, i=None):
        if i:
            self.stack[self.frame] += i
            if self.inthigh < self.stack[self.frame]:
                self.inthigh = self.stack[self.frame]
        else:
            return self.stack[self.frame]

    def begin(self):
        self.frame += 1
        if self.frame == len(self.stack):
            a = range(2*self.frame)
            a = [deepcopy(stack[x]) for x in range(self.frame)]
            # System.arraycopy(self.stack,0,a,0,self.frame)
            self.stack = a
        self.stack[self.frame] = self.stack[self.frame-1]
        return self.i()

    def rollback(self):
        self.stack[self.frame] = 0 if self.frame == 0 else self.stack[self.frame-1]

    def success(self, t=None):
        if t:
            self.success()
            return t
        else:
            self.frame -= 1
            self.stack[self.frame] = self.stack[self.frame+1]
            return True

    def failure(self, t=None):
        if t:
            self.failure()
            return t
        else:
            self.frame -= 1
            return False

    def currentindex(self):
        return self.i()

    def highindex(self):
        return self.inthigh

    def lastchar(self):
        return self.text[self.i()-1:self.i()]

    def currentchar(self):
        return self.text[self.i():self.i()+1]

    def endofinput(self):
        return self.i() >= self.length

    def match(self, char=None, string=None):
        if char:
            if self.endofinput() or self.text[self.i():self.i()+1] != char:
                return False
            self.i(1)
            return True
        if string:
            n = len(string)
            if not self.text.regionmatches(self.i(),string,n):
                return False
            self.i(n)
            return True

    def matchignorecase(self, s):
        n = len(s)
        if not self.text.regionmatches(self.i(),s,n):
            return False
        self.i(n)
        return True

    def anyof(self, s):
        if self.endofinput() or s.find(self.text[self.i():self.i()+1]) == -1:
            return False
        self.i(1)
        return True

    def noneof(self, s):
        if self.endofinput() or s.find(self.text[self.i():self.i()+1]) != -1:
            return False
        self.i(1)
        return True

    def incharrange(self, cLow, cHigh):
        if self.endofinput():
            return False
        c = self.text[self.i():self.i()+1]
        if c < cLow or c > cHigh:
            return False
        self.i(1)
        return True

    def anychar(self):
        if self.endofinput():
            return False
        self.i(1)
        return True

    def test(self, char=None, string=None):
        if char:
            return not self.endofinput() and self.text[self.i():self.i()+1] == char
        if string:
            return self.text.regionmatches(self.i(),string,len(string))

    def test(self, string):
        return self.text[self.i(),string,len(string)]

    def testignorecase(self, string):
        return self.text.regionmatches(self.i(),string,len(string))

    def textfrom(self, start):
        return self.text[start:self.i()]
    
    def regionmatches(self,start,string,numofchar):
        return string in self.text[start:numofchar]




