def lookup(name):
	names = name.split('.')
	top = __import__(names[0])
	for name in names[1:]:
		top = getattr(top, name)
	return top


from java.util.zip import ZipFile
zipfiles = {}
def getzip(filename):
	if zipfiles.has_key(filename):
		return zipfiles[filename]
	zipfile = ZipFile(filename)
	zipfiles[filename] = zipfile
	return zipfile
	
def closezips():
	for zf in zipfiles.values():
		zf.close()
	zipfiles.clear()