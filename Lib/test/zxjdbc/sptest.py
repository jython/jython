
# Jython Database Specification API 2.0
#
# $Id$
#
# Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>

import sys
from zxtest import zxJDBCTest
from com.ziclix.python.sql import Procedure

class SPTest(zxJDBCTest):
	"""
	These tests are very specific to Oracle.  Eventually support for other engines will
	be available.
	"""

	def setUp(self):
		zxJDBCTest.setUp(self)

		c = self.cursor()

		try:
			try:
				c.execute("drop table plsqltest")
			except:
				self.db.rollback()
			try:
				c.execute("create table plsqltest (x char(20))")
				c.execute("create or replace procedure procnone is begin insert into plsqltest values ('testing'); end;")
				c.execute("create or replace procedure procin (y in char) is begin insert into plsqltest values (y); end;")
				c.execute("create or replace procedure procout (y out char) is begin y := 'tested'; end;")
				c.execute("create or replace procedure procinout (y out varchar, z in varchar) is begin insert into plsqltest values (z); y := 'tested'; end;")
				c.execute("create or replace function funcnone return char is begin return 'tested'; end;")
				c.execute("create or replace function funcin (y char) return char is begin return y || y; end;")
				c.execute("create or replace function funcout (y out char) return char is begin y := 'tested'; return 'returned'; end;")
				c.execute("create or replace function raisesal (name char, raise number) return number is begin return raise + 100000; end;")
				self.db.commit()
			except:
				self.db.rollback()
				fail("procedure creation failed")

			self.proc_errors("PROC")
			self.proc_errors("FUNC")
			self.proc_errors("RAISESAL")

		finally:
			c.close()

	def tearDown(self):
		zxJDBCTest.tearDown(self)

	def proc_errors(self, name):
		c = self.cursor()
		try:
			c.execute("select * from user_errors where name like '%s%%'" % (name))
			errors = c.fetchall()
			try:
				assert errors is None, "found errors"
			except AssertionError, e:
				for a in errors:
					print a
				raise e
		finally:
			c.close()

	def testProcin(self):
		c = self.cursor()
		try:
			c.callproc("procin", ("testProcin",))
			self.assertEquals(None, c.fetchall())
			c.execute("select * from plsqltest")
			self.assertEquals(1, len(c.fetchall()))
		finally:
			c.close()

	def testProcinout(self):
		c = self.cursor()
		try:
			c.callproc("procinout", ("testing",))
			data = c.fetchone()
			assert data is not None, "data was None"
			self.assertEquals("tested", data[0])
		finally:
			c.close()

	def testFuncnone(self):
		c = self.cursor()
		try:
			c.callproc("funcnone")
			data = c.fetchone()
			assert data is not None, "data was None"
			self.assertEquals(1, len(data))
			self.assertEquals("tested", data[0])
		finally:
			c.close()

	def testFuncin(self):
		c = self.cursor()
		try:
			c.callproc("funcin", ("testing",))
			self.assertEquals(1, c.rowcount)
			data = c.fetchone()
			assert data is not None, "data was None"
			self.assertEquals(1, len(data))
			self.assertEquals("testingtesting", data[0])
		finally:
			c.close()

	def testFuncout(self):
		c = self.cursor()
		try:
			c.callproc("funcout")
			data = c.fetchone()
			assert data is not None, "data was None"
			self.assertEquals(2, len(data))
			self.assertEquals("returned", data[0])
			self.assertEquals("tested", data[1].strip())
		finally:
			c.close()

	def testRaisesalary(self):
		c = self.cursor()
		try:
			c.callproc("raisesal", ("jython developer", 18000))
			data = c.fetchone()
			assert data is not None, "data was None"
			self.assertEquals(1, len(data))
			self.assertEquals(18000 + 100000, data[0])
		finally:
			c.close()


