# tool/java_object_gen Emit Java
#
# Copyright (c)2021 Jython Developers.
# Licensed to PSF under a contributor agreement.

# This is a tool used from the core.gradle build file to generate object
# implementation methods, such as __neg__ and __rsub__, in Java.
# It processes Java files looking for a few simple markers, which it
# replaces with blocks of method definitions.
#
# See the files in core/src/main/javaTemplate for examples.

import sys
import os
import re
import argparse
import srcgen
from re import match
from contextlib import closing
from dataclasses import dataclass

from core import ImplementationGenerator
from core import PyFloatGenerator
from core import PyLongGenerator
from core import PyUnicodeGenerator


class ImplementationTemplateProcessorFactory:
    "Class creating a processor for object templates"

    def __init__(self, source_dir, dest_dir, error, verbose=False):
        "Create a factory specifying source and destination roots"
        self.src_dir = os.path.relpath(source_dir)
        self.dst_dir = os.path.relpath(dest_dir)
        self.verbose = verbose
        # Check source directory
        if not os.path.isdir(self.src_dir):
            error(f'no such directory {self.src_dir}')
        # Ensure destination directory
        if not os.path.isdir(self.dst_dir):
            os.makedirs(self.dst_dir, exist_ok=True)
        # Confirm
        if self.verbose:
            # cwd is the project directory e.g. ~/rt3
            cwd = os.getcwd()
            print(f'  Current dir = {cwd}')
            print(f'  templates from {self.src_dir} to {self.dst_dir}')

    def get_processor(self, package, name):
        "Create a template processor for one named class"
        return ImplementationTemplateProcessor(self, package, name)


class ImplementationTemplateProcessor:
    "A template processor for one named class"

    # Patterns marker lines in template files.
    # Each has a group 1 that captures the indentation.
    OBJECT_GENERATOR = re.compile(
            r'([\t ]*)//\s*\$OBJECT_GENERATOR\$\s*(\w+)')
    SPECIAL_METHODS = re.compile(r'([\t ]*)//\s*\$SPECIAL_METHODS\$')
    SPECIAL_BINOPS = re.compile(r'([\t ]*)//\s*\$SPECIAL_BINOPS\$')
    MANGLED = re.compile(r'(([\t ]*)//\s*\($\w+\$)')

    def __init__(self, factory, package, name):
        self.factory = factory
        self.package = package
        self.name = name
        self.generatorClass = ImplementationGenerator
        self.generator = None
        self.emitterClass = srcgen.IndentedEmitter

    def open_src(self):
        return open(
            os.path.join(self.factory.src_dir, self.package, self.name),
                    'r', encoding='utf-8')

    def open_dst(self):
        location = os.path.join(self.factory.dst_dir, self.package)
        os.makedirs(location, exist_ok=True)
        return open(
            os.path.join(location, self.name),
                    'w', encoding='utf-8', newline='\n')

    def process(self):
        if self.factory.verbose:
            print(f"    process {self.name}")
        with self.open_src() as src:
            with self.open_dst() as dst:
                self.process_lines(src, dst)

    def process_lines(self, src, dst):

        def emitter(m):
            indent = (len(m[1].expandtabs(4)) + 3) // 4
            return self.emitterClass(dst, 70, indent)

        for line in src:

            if m := self.OBJECT_GENERATOR.match(line):
                generatorName = m[2]
                self.generatorClass = globals()[generatorName]
                self.generator = self.generatorClass()
                with closing(emitter(m)) as e:
                    self.generator.emit_object_template(e, src)

            elif m := self.SPECIAL_METHODS.match(line):
                with closing(emitter(m)) as e:
                    self.generator.special_methods(e)

            elif m := self.SPECIAL_BINOPS.match(line):
                with closing(emitter(m)) as e:
                    self.generator.special_binops(e)

            elif m := self.MANGLED.match(line):
                print("Mangled template directive?",
                        m[2], file=sys.stderr)
                dst.write(line)

            else:
                dst.write(line)


def get_parser():
    parser = argparse.ArgumentParser(
            prog='java_object_gen',
            description='Generate Python object implementations.'
        )

    parser.add_argument('source_dir',
            help='Template directory (to process)')
    parser.add_argument('dest_dir',
            help='Destination directory (in build tree)')
    parser.add_argument('--verbose', '-v', action='store_true',
            help='Show more information')
    return parser


def process(src_dir, dest_dir, error, verbose=False):
    '''Friendly entry point to use this script via API.'''
    # Embed arguments into factory
    factory = ImplementationTemplateProcessorFactory(
            src_dir, dest_dir, error, verbose)

    # Process all Java files in the template tree at src_dir
    for dirpath, dirnames, filenames in os.walk(src_dir):
        # Any .java files here?
        javanames = [n for n in filenames
                        if os.path.splitext(n)[1].lower() == '.java']
        if javanames:
            package = os.path.relpath(dirpath, src_dir)
            for name in javanames:
                proc = factory.get_processor(package, name)
                proc.process()


def main():
    # Parse the command line to argparse arguments
    parser = get_parser()
    args = parser.parse_args()
    process(args.source_dir, args.dest_dir, parser.error, args.verbose)


if __name__ == '__main__':
    main()

