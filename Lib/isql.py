# $Id$

import dbexts, cmd, sys, os

"""
Isql works in conjunction with dbexts to provide an interactive environment
for database work.
"""

__version__ = "$Revision$"[11:-2]

class IsqlExit(Exception): pass

class Prompt:
	"""
	This class fixes a problem with the cmd.Cmd class since it uses an ivar 'prompt'
	as opposed to a method 'prompt()'.  To get around this, this class is plugged in
	as a 'prompt' attribute and when invoked the '__str__' method is called which
	figures out the	appropriate prompt to display.  I still think, even though this
	is clever, the attribute version of 'prompt' is poor design.
	"""
	def __init__(self, isql):
		self.isql = isql
	def __str__(self):
		prompt = "%s> " % (self.isql.db.dbname)
		if len(self.isql.sqlbuffer) > 0:
			prompt = "... "
		return prompt
	if os.name == 'java':
		def __tojava__(self, cls):
			import java
			if cls == java.lang.String:
				return self.__str__()
			return None

class IsqlCmd(cmd.Cmd):

	def __init__(self, db=None, delimiter=";"):
		cmd.Cmd.__init__(self)
		if db is None or type(db) == type(""):
			self.db = dbexts.dbexts(db)
		else:
			self.db = db
		self.kw = {}
		self.sqlbuffer = []
		self.delimiter = delimiter
		self.prompt = Prompt(self)

	def do_which(self, arg):
		"""\nPrints the current db connection parameters.\n"""
		print self.db
		return None

	def do_EOF(self, arg):
		return None

	def do_p(self, arg):
		"""\nExecute a python expression.\n"""
		try:
			exec arg.strip() in globals()
		except:
			print sys.exc_info()[1]
		return None

	def do_use(self, arg):
		"""\nUse a new database connection.\n"""
		# this allows custom dbexts
		self.db = self.db.__class__(arg.strip())
		return None

	def do_table(self, arg):
		"""\nPrints table meta-data.  If no table name, prints all tables.\n"""
		if len(arg.strip()):
			self.db.table(arg, **self.kw)
		else:
			self.db.table(None, **self.kw)
		return None

	def do_proc(self, arg):
		"""\nPrints store procedure meta-data.\n"""
		if len(arg.strip()):
			self.db.proc(arg, **self.kw)
		else:
			self.db.proc(None, **self.kw)
		return None

	def do_schema(self, arg):
		"""\nPrints schema information.\n"""
		print
		self.db.schema(arg)
		print
		return None

	def do_delimiter(self, arg):
		"""\nChange the delimiter.\n"""
		delimiter = arg.strip()
		if len(delimiter) > 0:
			self.delimiter = delimiter

	def do_q(self, arg):
		"""\nQuit.\n"""
		return 1

	def do_set(self, arg):
		"""\nSet a parameter. Some examples:\n set owner = 'informix'\n set types = ['VIEW', 'TABLE']\nThe right hand side is evaluated using `eval()`\n"""
		if len(arg.strip()) == 0:
			items = self.kw.items()
			if len(items):
				print
				# format the results but don't include how many rows affected
				for a in dbexts.console(items, ("key", "value"))[:-1]:
					print a
				print
			return None
		d = filter(lambda x: len(x) > 0, map(lambda x: x.strip(), arg.split("=")))
		if len(d) == 1:
			if self.kw.has_key(d[0]):
				del self.kw[d[0]]
		else:
			self.kw[d[0]] = eval(d[1])

	def default(self, arg):
		try:
			token = arg.strip()
			if token[0] == '\\':
				token = token[1:]
			# is it possible the line contains the delimiter
			if len(token) >= len(self.delimiter):
				# does the line end with the delimiter
				if token[-1 * len(self.delimiter):] == self.delimiter:
					# now add all up to the delimiter
					self.sqlbuffer.append(token[:-1 * len(self.delimiter)])
					if self.sqlbuffer:
						self.db.isql(" ".join(self.sqlbuffer), **self.kw)
						self.sqlbuffer = []
						if self.db.updatecount:
							print
							if self.db.updatecount == 1:
								print "1 row affected"
							else:
								print "%d rows affected" % (self.db.updatecount)
							print
						return None
			if token:
				self.sqlbuffer.append(token)
		except:
			self.sqlbuffer = []
			print
			print sys.exc_info()[1]
			print
		return None

	def emptyline(self):
		return None

	def postloop(self):
		raise IsqlExit()

	def cmdloop(self, intro=None):
		while 1:
			try:
				cmd.Cmd.cmdloop(self, intro)
			except IsqlExit, e:
				break
			except Exception, e:
				print
				print e
				print
			intro = None

if __name__ == '__main__':
	import getopt

	try:
		opts, args = getopt.getopt(sys.argv[1:], "b:", [])
	except getopt.error, msg:
		print
		print msg
		print "Try `%s --help` for more information." % (sys.argv[0])
		sys.exit(0)

	dbname = None
	for opt, arg in opts:
		if opt == '-b':
			dbname = arg

	intro = "\nisql - interactive sql (%s)\n" % (__version__)

	isql = IsqlCmd(dbname)
	isql.cmdloop()
