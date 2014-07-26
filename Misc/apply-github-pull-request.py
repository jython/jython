#!/usr/bin/env python
# -*- coding: utf-8 -*-

'''
Helper script to apply pull requests from github.com to local hg working copy.

Example:

cd jython-hg-workdir
apply-github-pull-request.py 4

'''

import argparse
import getpass
import os
import requests
import subprocess
import sys
import tempfile


def get_pull_url(repo, pr_id):
    return 'https://github.com/{}/pull/{}'.format(repo, pr_id)


def get_added_files(diff):
    '''hacky approach to extract added files from github diff output'''

    prefix = '+++ b/'
    lastline = None
    for line in diff.splitlines():
        line = line.strip()
        if line.startswith(prefix) and lastline and lastline == '--- /dev/null':
            yield line[len(prefix):]
        lastline = line


def main(args):

    if not os.path.exists('.hg'):
        print 'ERROR: No .hg folder found.'
        print 'Please make sure you run this script from within your local hg.python.org/jython checkout.'
        return 1

    password = args.github_password

    if password and password.startswith('@'):
        # if the command line password starts with "@", we read it from a file
        # (this prevents exposing the password on the command line / bash history)
        with open(password[1:]) as fd:
            password = fd.read().strip()

    if not password:
        password = getpass.getpass('{}@github.com: '.format(args.github_user))

    from requests.auth import HTTPBasicAuth
    auth = HTTPBasicAuth(args.github_user, password)
    r = requests.get('https://api.github.com/repos/{}/pulls/{}'.format(args.github_repo, args.pull_request_id),
                     auth=auth)
    if r.status_code != 200:
        print 'ERROR:'
        print r.json()
        return 1

    data = r.json()

    r = requests.get('https://api.github.com/users/{}'.format(data['user']['login']), auth=auth)
    if r.status_code != 200:
        print 'ERROR:'
        print r.json()
        return 1

    user_data = r.json()
    commiter = '{} <{}>'.format(user_data['name'], user_data['email'])

    print 'Pull Request {} by {}:'.format(data['number'], commiter)
    print data['title']
    print data['body']
    print '-' * 40

    r = requests.get('{}.diff'.format(get_pull_url(args.github_repo, args.pull_request_id)))

    patch_contents = r.text

    print patch_contents

    added_files = set(get_added_files(patch_contents))

    with tempfile.NamedTemporaryFile(suffix='-github-patch-{}'.format(args.pull_request_id)) as fd:
        fd.write(patch_contents)
        fd.flush()
        cmd = [
            'patch',
            '-p1',
            '--batch',
            '--forward',
            '-i',
            fd.name,
        ]
        if args.dry_run:
            cmd.append('--dry-run')
        p = subprocess.Popen(cmd)
        p.communicate()

    print '-' * 40
    print 'Applied pull request {} into your current working directory.'.format(args.pull_request_id)
    print 'Please check the changes (run unit tests) and commit...'
    print '-' * 40
    for fn in sorted(added_files):
        print 'hg add {}'.format(fn)
    print 'hg ci -u "{}" -m "{} ({})"'.format(commiter, data['title'], get_pull_url(args.github_repo,
                                              args.pull_request_id))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--github-repo', default='jythontools/jython')
    parser.add_argument('-u', '--github-user', default=getpass.getuser())
    parser.add_argument('-p', '--github-password',
                        help='Your github password (you can use "@" to read the password from a file)')
    parser.add_argument('--dry-run', action='store_true', help='Dry-run mode: only show what would be done')
    parser.add_argument('pull_request_id', help='Pull request ID on github.com')

    args = parser.parse_args()
    sys.exit(main(args))
