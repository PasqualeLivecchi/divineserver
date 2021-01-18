import urllib.parse


def urlencode(string):
    try:
        return urllib.parse.quote(str(string,encoding="UTF-8"))
    except UnicodeEncodeError as uee:
        raise RuntimeError(uee)


def add(kv,key,value):
    current = kv.get(key)
    if current == None:
        kv[key] = value
    elif isinstance(current,list):
        lst = current
        lst.append(value)
    else:
        lst = []
        lst.append(current)
        lst.append(value)
        kv[key] = lst


def byte2string(bytearr,charset): # throws UnsupportedEncodingException {
    if charset != None:
        return str(bytearr,encoding=charset)
    arrchar = []
    for ba in bytearr:
        arrchar.append(ba)
    return ''.join(arrchar)


def string2bytes(string):
    b = bytes(len(string))
    for i in range(len(string)):
        b[i] = string[i]
    return b

