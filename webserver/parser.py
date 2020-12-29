from copy import deepcopy

class Parser:
    text = ""
    length = 0
    stack = [0 for i in range(256)]
    frame = 0
    inthigh = 0

    def __init__(self, text, encoding='utf-8'):
        if isinstance(text,str):
            self.text = text
            self.length = len(text)
            # print(f"parser __init__ text:{self.text} length:{self.length}")
        else:
            raise ValueError("String required")

    def i(self, i=None):
        if i:
            self.stack[self.frame] += i
            # print(f"i cur inthigh:{self.inthigh} frame:{self.frame} stack[frame]:{self.stack[self.frame]}")
            if self.inthigh < self.stack[self.frame]:
                self.inthigh = self.stack[self.frame]
                # print(f"set inthigh:{self.inthigh}")
        else:
            return self.stack[self.frame]

    def begin(self):
        # print("parser begin")
        self.frame += 1
        if self.frame == len(self.stack):
            a = range(2*self.frame)
            a = [deepcopy(stack[x]) for x in range(self.frame)]
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
        return self.inthigh

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
            # print(f"match char:'{char}' textchar:'{self.text[self.i()]}' endofinput:{self.endofinput()} textchar!=char:{self.text[self.i()] != char} text:{self.text}")
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
        # print(f"noneof self.endofinput():{self.endofinput()} s.find(self.text[self.i():self.i()+1])!=-1{s.find(self.text[self.i()]) != -1}")
        if self.endofinput() or s.find(self.text[self.i()]) != -1:
            return False
        self.i(1)
        return True

    def incharrange(self, clow, chigh):
        # print("parser incharrange")
        # cl,ch = ord(clow),ord(chigh)
        if self.endofinput():
            print(f"parser incharrange endofinput:{self.endofinput()}")
            return False
        c = chr(ord(self.text[self.i()]))#:self.i()+1]
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
        # print(f"textfrom start: {start} self.text[start:self.i()]:{self.text[start:self.i()]}")
        return self.text[start:self.i()]
    
    def regionmatches(self,start,string,numofchar):
        # print(f"parser regionmatches '{start}' '{string}' '{numofchar}'")
        # print(f"string[0:numofchar]:'{string[0:numofchar]}'")
        # print(f"self.text[start:start+numofchar]:'{self.text[start:start+numofchar]}'")
        # print(f"string[0:numofchar] == self.text[start:start+numofchar]:{string[0:numofchar] == self.text[start:start+numofchar]}")
        return string[0:numofchar] == self.text[start:start+numofchar]