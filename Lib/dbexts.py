# $Id$

"""
This script provides platform independence by wrapping Python
Database API 2.0 compatible drivers to allow seamless database
usage across implementations.

In order to use the C version, you need mxODBC and mxDateTime.
In order to use the Java version, you need zxJDBC.

>>> import dbexts
>>> d = dbexts.dbexts() # use the default db
>>> d.isql('select count(*) count from player')

count
-------
13569.0

1 row affected

>>> r = d.raw('select count(*) count from player')
>>> r
([('count', 3, 17, None, 15, 0, 1)], [(13569.0,)])
>>>

The configuration file follows the following format in a file name dbexts.ini:

[default]
name=development

[jdbc]
name=development
url=jdbc:db:devurl
username=root
password=12345

[jdbc]
name=production
url=jdbc:db:produrl
username=root
password=12345

[odbc]
name=development
username=root
password=12345
"""

import os, string, re

__author__ = "brian zimmer (bzimmer@ziclix.com)"
__version__ = "$Revision$"[11:-2]

__OS__ = os.name

choose = lambda bool, a, b: (bool and [a] or [b])[0]

def console(rows, headers=()):
	"""Format the results into a list	of strings (one for each row):

	<header>
	<headersep>
	<row1>
	<row2>
	...

	headers may be given as list of strings.

	Columns are separated by colsep; the header is separated from
	the result set by a line of headersep characters.

	The function calls stringify to format the value data into a string.
	It defaults to calling str() and striping leading and trailing whitespace.

	- copied and modified from mxODBC
	"""

	# Check row entry lengths
	output = []
	headers = map(string.upper, list(headers))
	collen = map(len,headers)
	output.append(headers)
	if rows and len(rows) > 0:
		for row in rows:
			row = map(lambda x: str(x), row)
			for i in range(len(row)):
				entry = row[i]
				if collen[i] < len(entry):
					collen[i] = len(entry)
			output.append(row)
	if len(output) == 1:
		affected = "0 rows affected"
	elif len(output) == 2:
		affected = "1 row affected"
	else:
		affected = "%d rows affected" % (len(output) - 1)

	# Format output
	for i in range(len(output)):
		row = output[i]
		l = []
		for j in range(len(row)):
			l.append('%-*s' % (collen[j],row[j]))
		output[i] = string.join(l, " | ")

	# Insert header separator
	totallen = len(output[0])
	output[1:1] = ["-"*(totallen/len("-"))]
	output.append("\n" + affected)
	return output

def html(rows, headers=()):
	output = []
	output.append('<table class="results">')
	output.append('<tr class="headers">')
	headers = map(lambda x: '<td class="header">%s</td>' % (x.upper()), list(headers))
	map(output.append, headers)
	output.append('</tr>')
	if rows and len(rows) > 0:
		for row in rows:
			output.append('<tr class="row">')
			row = map(lambda x: '<td class="value">%s</td>' % (x), row)
			map(output.append, row)
			output.append('</tr>')
	output.append('</table>')
	return output

comments = lambda x: re.compile("{.*?}", re.S).sub("", x, 0)

class ex_proxy:
	"""Wraps mxODBC to provide proxy support for zxJDBC's additional parameters."""
	def __init__(self, c):
		self.c = c
	def __getattr__(self, name):
		if name == "execute":
			return self.execute
		elif name == "gettypeinfo":
			return self.gettypeinfo
		else:
			return getattr(self.c, name)
	def execute(self, sql, params=None, bindings=None, maxrows=None):
		if params:
			self.c.execute(sql, params)
		else:
			self.c.execute(sql)
	def gettypeinfo(self, typeid=None):
		if typeid:
			self.c.gettypeinfo(typeid)

class executor:
	"""Handles the insertion of values given dynamic data."""
	def __init__(self, table, cols):
		self.cols = cols
		self.table = table
		if self.cols:
			self.sql = "insert into %s (%s) values (%s)" % (table, ",".join(self.cols), ",".join(("?",) * len(self.cols)))
		else:
			self.sql = "insert into %s values (%%s)" % (table)
	def execute(self, db, rows, bindings):
		assert rows and len(rows) > 0, "must have at least one row"
		if self.cols:
			sql = self.sql
		else:
			sql = self.sql % (",".join(("?",) * len(rows[0])))
		db.raw(sql, rows, bindings)

def connect(dbname):
	return dbexts(dbname)

def lookup(dbname):
	return dbexts(jndiname=dbname)

class dbexts:
	def __init__(self, dbname=None, cfg=None, formatter=console, autocommit=1, jndiname=None, out=None):
		self.verbose = 1
		self.results = None
		self.headers = None
		self.datahandler = None
		self.autocommit = autocommit
		self.formatter = formatter
		self.out = out
		self.metaDataCase = lambda x: x

		if not jndiname:
			if cfg == None: cfg = os.path.join(os.path.split(__file__)[0], "dbexts.ini")
			if isinstance(cfg, IniParser):
				self.dbs = cfg
			else:
				self.dbs = IniParser(cfg)
			if dbname == None: dbname = self.dbs[("default", "name")]

		if __OS__ == 'java':

			from com.ziclix.python.sql import zxJDBC
			database = zxJDBC
			if not jndiname:
				t = self.dbs[("jdbc", dbname)]
				self.dburl, dbuser, dbpwd, jdbcdriver = t['url'], t['user'], t['pwd'], t['driver']
				if t.has_key("datahandler"):
					try:
						datahandlerclass = string.split(t['datahandler'], ".")[-1]
						self.datahandler = __import__(t['datahandler'], globals(), locals(), datahandlerclass)
					except:
						pass
				if t.has_key("metaDataCase"):
					self.metaDataCase = getattr(string, t['metaDataCase'])
				keys = filter(lambda x: x not in ['url', 'user', 'pwd', 'driver', 'datahandler', 'name', 'metaDataCase'], t.keys())
				props = {}
				for a in keys:
					props[a] = t[a]
				self.db = apply(database.connect, (self.dburl, dbuser, dbpwd, jdbcdriver), props)
			else:
				self.db = database.lookup(jndiname)
			self.db.autocommit = 0

		elif __OS__ == 'nt':

			for modname in ["mx.ODBC.Windows", "ODBC.Windows"]:
				try:
					database = __import__(modname, globals(), locals(), "Windows")
					break
				except:
					continue
			else:
				raise ImportError("unable to find appropriate mx ODBC module")

			t = self.dbs[("odbc", dbname)]
			self.dburl, dbuser, dbpwd = t['url'], t['user'], t['pwd']
			self.db = database.Connect(self.dburl, dbuser, dbpwd, clear_auto_commit=1)

		for a in database.sqltype.keys():
			setattr(self, database.sqltype[a], a)
		del database

	def __str__(self):
		return self.dburl

	def __repr__(self):
		return self.dburl

	def __getattr__(self, name):
		if "cfg" == name:
			return self.dbs.cfg

	def close(self):
		""" close the connection to the database """
		self.db.close()

	def begin(self):
		""" reset ivars and return a new cursor, possibly binding an auxiliary datahandler """
		self.headers, self.results = None, None
		c = self.db.cursor()
		if __OS__ == 'java':
			if self.datahandler: c.datahandler = self.datahandler(c.datahandler)
		else:
			c = ex_proxy(c)
		return c

	def commit(self, cursor=None):
		""" commit the cursor and create the result set """
		if cursor and cursor.description:
			self.headers = cursor.description
			self.results = cursor.fetchall()
			if hasattr(cursor, "nextset"):
				s = cursor.nextset()
				while s:
					f = cursor.fetchall()
					if f: self.results = choose(self.results is None, [], self.results) + f
					s = cursor.nextset()
		if self.autocommit or cursor is None: self.db.commit()
		if cursor: cursor.close()

	def rollback(self):
		""" rollback the cursor """
		self.db.rollback()

	def display(self):
		""" using the formatter, display the results """
		if self.formatter and self.verbose > 0:
			res = self.results
			if res:
				print >> self.out, ""
				for a in self.formatter(res, map(lambda x: x[0], self.headers)):
					print >> self.out, a
				print >> self.out, ""

	def __execute__(self, sql, params=None, bindings=None, maxrows=None):
		""" the primary execution method """
		cur = self.begin()
		try:
			if bindings:
				cur.execute(sql, params, bindings, maxrows=maxrows)
			elif params:
				cur.execute(sql, params, maxrows=maxrows)
			else:
				cur.execute(sql, maxrows=maxrows)
		finally:
			self.commit(cur)

	def isql(self, sql, params=None, bindings=None, maxrows=None):
		""" execute and display the sql """
		self.raw(sql, params, bindings, maxrows=maxrows)
		self.display()

	def raw(self, sql, params=None, bindings=None, delim=None, comments=comments, maxrows=None):
		""" execute the sql and return a tuple of (headers, results) """
		if delim:
			headers = []
			results = []
			if comments: sql = comments(sql)
			statements = filter(lambda x: len(x) > 0, map(string.strip, string.split(sql, delim)))
			for a in statements:
				self.__execute__(a, params, bindings, maxrows=maxrows)
				headers.append(self.headers)
				results.append(self.results)
			self.headers = headers
			self.results = results
		else:
			self.__execute__(sql, params, bindings, maxrows=maxrows)
		return (self.headers, self.results)

	def pk(self, table, owner=None, schema=None):
		""" display the table's primary keys """
		cur = self.begin()
		cur.primarykeys(schema, owner, self.metaDataCase(table))
		self.commit(cur)
		self.display()

	def fk(self, primary_table=None, foreign_table=None, owner=None, schema=None):
		""" display the table's foreign keys """
		cur = self.begin()
		if primary_table and foreign_table:
			cur.foreignkeys(schema, owner, self.metaDataCase(primary_table), schema, owner, self.metaDataCase(foreign_table))
		elif primary_table:
			cur.foreignkeys(schema, owner, self.metaDataCase(primary_table), schema, owner, None)
		elif foreign_table:
			cur.foreignkeys(schema, owner, None, schema, owner, self.metaDataCase(foreign_table))
		self.commit(cur)
		self.display()

	def table(self, table=None, types=("TABLE",), owner=None, schema=None):
		"""If no table argument, displays a list of all tables.  If a table argument,
		displays the columns of the given table."""
		cur = self.begin()
		if table:
			cur.columns(schema, owner, self.metaDataCase(table), None)
		else:
			cur.tables(schema, owner, None, types)
		self.commit(cur)
		self.display()

	def proc(self, proc=None, owner=None, schema=None):
		"""If no proc argument, displays a list of all procedures.  If a proc argument,
		displays the parameters of the given procedure."""
		cur = self.begin()
		if proc:
			cur.procedurecolumns(schema, owner, proc, None)
		else:
			cur.procedures(schema, owner, None)
		self.commit(cur)
		self.display()

	def stat(self, table, qualifier=None, owner=None, unique=0, accuracy=0):
		""" display the table's indicies """
		cur = self.begin()
		cur.statistics(qualifier, owner, self.metaDataCase(table), unique, accuracy)
		self.commit(cur)
		self.display()

	def typeinfo(self, sqltype=None):
		""" display the types available for the database """
		cur = self.begin()
		cur.gettypeinfo(sqltype)
		self.commit(cur)
		self.display()

	def tabletypeinfo(self):
		""" display the table types available for the database """
		cur = self.begin()
		cur.gettabletypeinfo()
		self.commit(cur)
		self.display()

	def schema(self, table, full=0, sort=1, owner=None):
		"""Displays a Schema object for the table.  If full is true, then generates
		references to the table in addition to the standard fields.  If sort is true,
		sort all the items in the schema, else leave them in db dependent order."""
		print >> self.out, str(Schema(self, table, owner, full, sort))

	def bulkcopy(self, dst, table, include=[], exclude=[], autobatch=0, executor=executor):
		"""Returns a Bulkcopy object using the given table."""
		if type(dst) == type(""):
			dst = dbexts(dst, cfg=self.dbs)
		bcp = Bulkcopy(dst, table, include=include, exclude=exclude, autobatch=autobatch, executor=executor)
		return bcp

	def bcp(self, src, table, where='(1=1)', params=[], include=[], exclude=[], autobatch=0, executor=executor):
		"""Bulkcopy of rows from a src database to the current database for a given table and where clause."""
		if type(src) == type(""):
			src = dbexts(src, cfg=self.dbs)
		bcp = self.bulkcopy(self, table, include, exclude, autobatch, executor)
		num = bcp.transfer(src, where, params)
		return num

	def unload(self, filename, sql, delimiter=",", includeheaders=1):
		""" Unloads the delimited results of the query to the file specified, optionally including headers. """
		u = Unload(self, filename, delimiter, includeheaders)
		u.unload(sql)

class Bulkcopy:
	"""The idea for a bcp class came from http://object-craft.com.au/projects/sybase"""
	def __init__(self, dst, table, include=[], exclude=[], autobatch=0, executor=executor):
		self.dst = dst
		self.table = table
		self.total = 0
		self.rows = []
		self.autobatch = autobatch
		self.bindings = {}

		include = map(lambda x: string.lower(x), include)
		exclude = map(lambda x: string.lower(x), exclude)

		_verbose = self.dst.verbose
		self.dst.verbose = 0
		try:
			self.dst.table(self.table)
			if self.dst.results:
				colmap = {}
				for a in self.dst.results:
					colmap[a[3].lower()] = a[4]
				cols = self.__filter__(colmap.keys(), include, exclude)
				for a in zip(range(len(cols)), cols):
					self.bindings[a[0]] = colmap[a[1]]
				colmap = None
			else:
				cols = self.__filter__(include, include, exclude)
		finally:
			self.dst.verbose = _verbose

		self.executor = executor(table, cols)

	def __str__(self):
		return "[%s].[%s]" % (self.dst, self.table)

	def __repr__(self):
		return "[%s].[%s]" % (self.dst, self.table)

	def __getattr__(self, name):
		if name == 'columns':
			return self.executor.cols

	def __filter__(self, values, include, exclude):
		cols = map(string.lower, values)
		if exclude:
			cols = filter(lambda x, ex=exclude: x not in ex, cols)
		if include:
			cols = filter(lambda x, inc=include: x in inc, cols)
		return cols

	def format(self, column, type):
		self.bindings[column] = type

	def done(self):
		if len(self.rows) > 0:
			return self.batch()
		return 0

	def batch(self):
		self.executor.execute(self.dst, self.rows, self.bindings)
		cnt = len(self.rows)
		self.total += cnt
		self.rows = []
		return cnt

	def rowxfer(self, line):
		self.rows.append(line)
		if self.autobatch: self.batch()

	def transfer(self, src, where="(1=1)", params=[]):
		sql = "select %s from %s where %s" % (string.join(self.columns, ", "), self.table, where)
		h, d = src.raw(sql, params)
		if d:
			map(self.rowxfer, d)
			return self.done()
		return 0

class Unload:
	"""Unloads a sql statement to a file with optional formatting of each value."""
	def __init__(self, db, filename, delimiter=",", includeheaders=1):
		self.db = db
		self.filename = filename
		self.delimiter = delimiter
		self.includeheaders = includeheaders
		self.formatters = {}

	def format(self, o):
		if not o:
			return ""
		o = str(o)
		if o.find(",") != -1:
			o = "\"\"%s\"\"" % (o)
		return o

	def unload(self, sql, mode="w"):
		headers, results = self.db.raw(sql)
		w = open(self.filename, mode)
		if self.includeheaders:
			w.write("%s\n" % (string.join(map(lambda x: x[0], headers), self.delimiter)))
		if results:
			for a in results:
				w.write("%s\n" % (string.join(map(self.format, a), self.delimiter)))
		w.flush()
		w.close()

class Schema:
	"""Produces a Schema object which represents the database schema for a table"""
	def __init__(self, db, table, owner=None, full=0, sort=1):
		self.db = db
		self.table = table
		self.owner = owner
		self.full = full
		self.sort = sort
		_verbose = self.db.verbose
		self.db.verbose = 0
		try:
			if table: self.computeschema()
		finally:
			self.db.verbose = _verbose

	def computeschema(self):
		self.db.table(self.table, owner=self.owner)
		self.columns = []
		# (column name, type_name, size, nullable)
		if self.db.results:
			self.columns = map(lambda x: (x[3], x[5], x[6], x[10]), self.db.results)
			if self.sort: self.columns.sort(lambda x, y: cmp(x[0], y[0]))

		self.db.fk(None, self.table)
		# (pk table name, pk column name, fk column name, fk name, pk name)
		self.imported = []
		if self.db.results:
			self.imported = map(lambda x: (x[2], x[3], x[7], x[11], x[12]), self.db.results)
			if self.sort: self.imported.sort(lambda x, y: cmp(x[2], y[2]))

		self.exported = []
		if self.full:
			self.db.fk(self.table, None)
			# (pk column name, fk table name, fk column name, fk name, pk name)
			if self.db.results:
				self.exported = map(lambda x: (x[3], x[6], x[7], x[11], x[12]), self.db.results)
				if self.sort: self.exported.sort(lambda x, y: cmp(x[1], y[1]))

		self.db.pk(self.table)
		self.primarykeys = []
		if self.db.results:
			# (column name, key_seq, pk name)
			self.primarykeys = map(lambda x: (x[3], x[4], x[5]), self.db.results)
			if self.sort: self.primarykeys.sort(lambda x, y: cmp(x[1], y[1]))

		self.db.stat(self.table)
		# (non-unique, name, type, pos, column name, asc)
		self.indices = []
		if self.db.results:
			idxdict = {}
			# mxODBC returns a row of None's, so filter it out
			idx = map(lambda x: (x[3], string.strip(x[5]), x[6], x[7], x[8]), filter(lambda x: x[5], self.db.results))
			def cckmp(x, y):
				c = cmp(x[1], y[1])
				if c == 0: c = cmp(x[3], y[3])
				return c
			# sort this regardless, this gets the indicies lined up
			idx.sort(cckmp)
			for a in idx:
				if not idxdict.has_key(a[1]):
					idxdict[a[1]] = []
				idxdict[a[1]].append(a)
			self.indices = idxdict.values()
			if self.sort: self.indices.sort(lambda x, y: cmp(x[0][1], y[0][1]))

	def __str__(self):
		d = []
		d.append("Table")
		d.append("  " + self.table)
		d.append("\nPrimary Keys")
		for a in self.primarykeys:
			d.append("  %s {%s}" % (a[0], a[2]))
		d.append("\nImported (Foreign) Keys")
		for a in self.imported:
			d.append("  %s (%s.%s) {%s}" % (a[2], a[0], a[1], a[3]))
		if self.full:
			d.append("\nExported (Referenced) Keys")
			for a in self.exported:
				d.append("  %s (%s.%s) {%s}" % (a[0], a[1], a[2], a[3]))
		d.append("\nColumns")
		for a in self.columns:
			nullable = choose(a[3], "nullable", "non-nullable")
			d.append("  %-20s %s(%s), %s" % (a[0], a[1], a[2], nullable))
		d.append("\nIndices")
		for a in self.indices:
			unique = choose(a[0][0], "non-unique", "unique")
			cname = string.join(map(lambda x: x[4], a), ", ")
			d.append("  %s index {%s} on (%s)" % (unique, a[0][1], cname))
		return string.join(d, "\n")

class IniParser:
	def __init__(self, cfg, key='name'):
		self.key = key
		self.records = {}
		self.ctypeRE = re.compile("\[(jdbc|odbc|default)\]")
		self.entryRE = re.compile("([a-zA-Z]+)[ \t]*=[ \t]*(.*)")
		self.cfg = cfg
		self.parse()

	def parse(self):
		fp = open(self.cfg, "r")
		data = fp.readlines()
		fp.close()
		lines = filter(lambda x: len(x) > 0 and x[0] not in ['#', ';'], map(string.strip, data))
		current = None
		for i in range(len(lines)):
			line = lines[i]
			g = self.ctypeRE.match(line)
			if g:	# a section header
				current = {}
				if not self.records.has_key(g.group(1)):
					self.records[g.group(1)] = []
				self.records[g.group(1)].append(current)
			else:
				g = self.entryRE.match(line)
				if g:
					current[g.group(1)] = g.group(2)

	def __getitem__(self, (ctype, skey)):
		if skey == self.key: return self.records[ctype][0][skey]
		t = filter(lambda x, p=self.key, s=skey: x[p] == s, self.records[ctype])
		if not t or len(t) > 1:
			raise KeyError, "invalid key ('%s', '%s')" % (ctype, skey)
		return t[0]

def random_table_name(prefix, num_chars):
	import random
	d = [prefix, '_']
	i = 0
	while i < num_chars:
		d.append(chr(int(100 * random.random()) % 26 + ord('A')))
		i += 1
	return string.join(d, "")

class ResultSetRow:
	def __init__(self, rs, row):
		self.row = row
		self.rs = rs
	def __getitem__(self, i):
		if type(i) == type(""):
			i = self.rs.index(i)
		return self.row[i]
	def __getslice__(self, i, j):
		if type(i) == type(""): i = self.rs.index(i)
		if type(j) == type(""): j = self.rs.index(j)
		return self.row[i:j]
	def __len__(self):
		return len(self.row)
	def __repr__(self):
		return str(self.row)

class ResultSet:
	def __init__(self, headers, results=[]):
		self.headers = map(lambda x: x.upper(), headers)
		self.results = results
	def index(self, i):
		return self.headers.index(string.upper(i))
	def __getitem__(self, i):
		return ResultSetRow(self, self.results[i])
	def __getslice__(self, i, j):
		return map(lambda x, rs=self: ResultSetRow(rs, x), self.results[i:j])
	def __repr__(self):
		return "<%s instance {cols [%d], rows [%d]} at %s>" % (self.__class__, len(self.headers), len(self.results), id(self))
