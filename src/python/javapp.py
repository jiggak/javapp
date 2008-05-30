#
# Copyright 2008 Josh Kropf
# 
# This file is part of javapp.
# 
# javapp is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
# 
# javapp is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with javapp; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
#
import re
from StringIO import StringIO
from Plex import *

class CondState:
    def __init__(self, result):
        self.result = result
        self.closed = False
        self.buffer = StringIO()

    def write(self, s):
        if self.result and not self.closed:
            self.buffer.write(s)

    def next(self, result):
        if self.result:
            self.closed = True
        else:
            self.result = result

    def __eq__(self, other):
        return self.result == other

    def __str__(self):
        if self.result:
            return self.buffer.getvalue()
        else:
            return ""

class stack(list):
    def push(self, obj):
        self.insert(0, obj)

    def pop(self):
        return list.pop(self, 0)

    def peek(self):
        return self[0]

    def isempty(self):
        return len(self) == 0

class PpScanner(Scanner):

    def if_cond(self, text):
        self.cond_stack.append(CondState(eval(self.expand_cond(text))))
        return ""

    def elif_cond(self, text):
        self.cond_stack.peek().next(eval(self.expand_cond(text)))
        return ""

    def else_cond(self, text):
        self.cond_stack.peek().next(True)
        return ""

    def end_cond(self, text):
        return str(self.cond_stack.pop())

    def def_var(self, text):
        tokens = text.split(None, 1)
        if len(tokens) != 1:
            self.env[tokens[0]] = tokens[1]
        else:
            self.env[tokens[0]] = ""
        return ""

    def expand_cond(self, text):
        text = self.var_re.sub('self.env["\\1"]', text)
        text = self.ndef_re.sub('self.env.has_key("\\1") == False', text)
        text = self.def_re.sub('self.env.has_key("\\1")', text)
        return text

    def expand_var(self, text):
        return self.output(self.env[text])

    def output(self, text):
        if self.cond_stack.isempty():
            return text
        else:
            self.cond_stack.peek().write(text)
            return ""

    var_re = re.compile('\$\{([^\}]+)\}')
    def_re = re.compile('defined\(([^\)]+)\)')
    ndef_re = re.compile('!defined\(([^\)]+)\)')

    comp_op  = Str("==") | Str("!=") | Str(">") | Str("<") | Str(">=") | Str("<=")
    logic_op = Str("or") | Str("and")

    prefix = Str("#")
    zspace = Rep(Any(" \t"))
    space  = Rep1(Any(" \t"))

    letter   = Range("AZaz") | Any("_.")
    digit    = Range("09")
    hexdigit = Range("09AFaf")

    ident  = letter + Rep(letter | digit)
    number = Rep1(digit) | (Str("0x") + Rep1(hexdigit))
    strlit = Str('"') + Rep(AnyBut('"')) + Str('"')

    var     = Str("${") + ident + Str("}")
    operand = strlit | number | var
    defexpr = Opt(Str("!")) + Str("defined(") + ident + Str(")")
    expr    = (operand + zspace + comp_op + zspace + operand) | defexpr
    cond    = expr + Rep(zspace + logic_op + zspace + expr)

    lexicon = Lexicon([
        (Str("${"), Begin('expand')),
        State('expand', [
            (ident,    expand_var),
            (Str("}"), Begin(''))
        ]),
        (prefix + Str("if"), Begin('ifcond')),
        State('ifcond', [
            (space,  IGNORE),
            (cond,   if_cond),
            (Eol,    Begin(''))
        ]),
        (prefix + Str("elif"), Begin('elifcond')),
        State('elifcond', [
            (space,  IGNORE),
            (cond,   elif_cond),
            (Eol,    Begin(''))
        ]),
        (prefix + Str("else"), else_cond),
        (prefix + Str("endif"), end_cond),
        (prefix + Str("define"), Begin('defvar')),
        State('defvar', [
            (space, IGNORE),
            (ident + Opt(space + Rep(AnyBut("\n"))), def_var),
            (Eol,   Begin(''))
        ]),
        (AnyChar,  output)
    ])

    def __init__(self, file, env):
        Scanner.__init__(self, self.lexicon, file)
        self.env = env
        self.cond_stack = stack()
        self.begin("")

def process(input, output, env = {}):
    scanner = PpScanner(input, env)
    while 1:
        token, text = scanner.read()
        if token is None:
            break
        output.write(token)

#if __name__ == '__main__':
#    import sys
#    process(sys.stdin, sys.stdout, {'poo': 'this is poo', 'pee': 'this is pee'})
