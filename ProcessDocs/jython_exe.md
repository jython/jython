# Regenerating the Windows Launcher `jython.exe`


`src/shell/jython.exe` is the Windows Jython launcher.
It is copied during the `ant` build to :file:`dist/bin`.
However, it is derived from :file:`src/shell/jython.py` using
[PyInstaller]( http://www.pyinstaller.org)
by the following process.

You need a Python 2.7 environment to accomplish this.

If it is not already installed, install `virtualenv`
with the command `pip install virtualenv`.

In any convenient working directory,
create a Python 2.7 virtual environment, activate it,
and install `PyInstaller`:
```
> virtualenv venv
New python executable in ... venv\Scripts\python.exe
Installing setuptools, pip, wheel...done.
> .\venv\Scripts\activate
(venv) > pip install "pyinstaller < 4"
Collecting pyinstaller
...
Installing collected packages: future, pefile, ...
Successfully installed ... pyinstaller-3.6
```

The above set-up need only be performed once for the virtual environment.

Copy `src/shell/jython.py` to this working directory.
Use `PyInstaller` to create a single-file executable,
and copy that back to :file:`src/shell`:
```
(venv) > copy <checkoutdir>\src\shell\jython.py .
(venv) > pyinstaller --onefile jython.py
...
(venv) > copy .\dist\jython.exe <checkoutdir>\src\shell
```
Above, ``<checkoutdir>`` stands for the root directory of the Jython source.

You *could* do all this in the source tree at `src/shell`,
but the virtual environment and `PyInstaller` leave a lot of
working material behind, so it is best done elsewhere.
