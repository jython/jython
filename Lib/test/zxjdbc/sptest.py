
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
				c.execute("create or replace procedure procin (y char) is begin insert into plsqltest values (y); end;")
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

	def testProcinout(self):
		c = self.cursor()
		try:
			p = Procedure(c, "procinout")
			stmt = p.prepareCall()
			params = ["testing"]
			params = p.normalizeParams(params)

			self.assertEquals(2, len(params))
			assert params[0] == p.PLACEHOLDER
			assert params[1] == "testing"
		finally:
			if stmt:
				stmt.close()
			c.close()

	def testFuncout(self):
		c = self.cursor()
		try:
			p = Procedure(c, "funcout")
			stmt = p.prepareCall()
			stmt.execute()
			self.assertEquals("returned", stmt.getString(1).strip())
			self.assertEquals("tested", stmt.getString(2).strip())
		finally:
			if stmt:
				stmt.close()
			c.close()

	def testProcinoutCall(self):
		c = self.cursor()
		try:
			c.callproc("procinout", ("testing",))
			assert c.fetchall() == None, "expected None"
		finally:
			c.close()

	def testFuncnoneCall(self):
		c = self.cursor()
		try:
			c.callproc("funcnone")
			assert c.fetchall() == None, "expected None"
		finally:
			c.close()

	def testFuncinCall(self):
		c = self.cursor()
		try:
			c.callproc("funcin", ("testing",))
			assert c.fetchall() == None, "expected None"
		finally:
			c.close()

	def testFuncoutCall(self):
		c = self.cursor()
		try:
			c.callproc("funcout")
			assert c.fetchall() == None, "expected None"
		finally:
			c.close()

	def testRaisesalaryCall(self):
		c = self.cursor()
		try:
			c.callproc("raisesal", ("jython developer", 18000))
			assert c.fetchall() == None, "expected None"
		finally:
			c.close()


