"""Generates the www.jython.org website style
"""

import os
from Skeleton import Skeleton
from Sidebar import Sidebar, BLANKCELL
from Banner import Banner
from HTParser import HTParser
from LinkFixer import LinkFixer


class JyGenerator(Skeleton, Sidebar, Banner):

    sitelinks = [
        ('%(rootdir)s/',              'Home'),
        ('http://www.python.org/',    'www.python.org'),
        ('%(rootdir)s/download.html', 'Download'),
        ('%(rootdir)s/docs/index.html',         'Documentation'),
    #    ('%(rootdir)s/applets/index.html',      'Applet Demos'),
        ]

    webmaster = 1 # webmaster mailto:jython-dev
    
    def __init__(self, file, rootdir, relthis):
        root, ext = os.path.splitext(file)
        html = root + '.html'
        p = self.__parser = HTParser(file,'x@y')
        f = self.__linkfixer = LinkFixer(html, rootdir, relthis)
        self.__body = None
        self.__cont = None
        
        # calculate the sidebar links, adding a few of our own
        self._d = {'rootdir': rootdir}
        p.process_sidebar()

        # remove "Email us" and use replacement
        del p.sidebar[-2:]

        p.sidebar.append('Contact')
        p.sidebar.append(('http://lists.sourceforge.net/lists/listinfo/jython-users','Questions on Jython?<br>jython-users'))
        
        p.sidebar.append(BLANKCELL)
        # it is important not to have newlines between the img tag and the end
        # end center tags, otherwise layout gets messed up
        p.sidebar.append(('http://www.python.org/', '''
<center>
    <img border="0" src="%(rootdir)s/images/PythonPoweredSmall.gif"></center>
''' % self._d))
        p.sidebar.append(BLANKCELL)
        p.sidebar.append(('http://sourceforge.net/', '''
<center>
 <img src="http://sourceforge.net/sflogo.php?group_id=12867" width="88" height="31" border="0" alt="SourceForge Logo"></center>
''' % self._d))

        if self.webmaster:
            # webmaster -> jython-dev

            p.sidebar.append(BLANKCELL)
            p.sidebar.append(BLANKCELL)

            p.sidebar.append(('mailto:jython-dev@lists.sf.net','webmaster'))

        self.__linkfixer.massage(p.sidebar, self._d)
        Sidebar.__init__(self, p.sidebar)
        #
        # fix up our site links, no relthis because the site links are
        # relative to the root of our web pages
        #
        sitelink_fixer = LinkFixer(f.myurl(), rootdir)
        sitelink_fixer.massage(self.sitelinks, self._d, aboves=1)
        Banner.__init__(self, self.sitelinks, cols=2)

    def get_title(self):
        return self.__parser.get('title')

    def get_sidebar(self):
        if self.__parser.get('wide-page', 'no').lower() == 'yes':
            return None
        return Sidebar.get_sidebar(self)

    def get_banner(self):
        return Banner.get_banner(self)

    def get_corner(self):
        # it is important not to have newlines between the img tag and the end
        # anchor and end center tags, otherwise layout gets messed up
        return '''
<center>
    <a href="%(rootdir)s/">
    <img border="0" src="%(rootdir)s/images/jython-new-small.gif"></a></center>''' % \
    self._d

    def get_body(self):
        self.__grokbody()
        return self.__body

    def get_cont(self):
        self.__grokbody()
        return self.__cont

    def __grokbody(self):
        if self.__body is None:
            text = self.__parser.fp.read()
            i = text.find('<!--table-stop-->')
            if i >= 0:
                self.__body = text[:i]
                self.__cont = text[i+17:]
            else:
                # there is no wide body
                self.__body = text

    # python.org color scheme overrides
    def get_vlinkcolor(self):
        return '#00000'

    def get_lightshade(self):
        return '#cccccc'

    def get_mediumshade(self):
        raise "what? mediumshade"

    def get_darkshade(self):
        return '#666699'
