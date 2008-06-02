#def stol(s)
    from java.security import MessageDigest

    md = MessageDigest.getInstance("SHA")
    md.update(s)
    bytes = md.digest()

    val = long(0L)
    for i in range(8):
        val = val | ((long(bytes[i]) & 0xff) << (8 * i))

    return "0x%xL" % val
#end
