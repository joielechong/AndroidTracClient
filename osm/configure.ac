#                                               -*- Autoconf -*-
# Process this file with autoconf to produce a configure script.

AC_PREREQ(2.63)
AC_INIT([osm],[0.1])
AM_INIT_AUTOMAKE(osm,0.1)
AC_CONFIG_SRCDIR([mkdb.cpp])
AC_CONFIG_HEADERS([config.h])
AC_LANG([C++])
AC_CONFIG_MACRO_DIR([m4])

# Checks for programs.
AC_PROG_CC
gl_EARLY
AC_PROG_CXX
AC_PROG_CPP
AC_PROG_CXXCPP
AC_PROG_INSTALL
AC_PROG_LIBTOOL
AC_PROG_AWK
AC_PROG_LN_S
AC_PROG_MAKE_SET
AC_PROG_YACC
AC_PROG_LEX

gl_INIT

# Checks for libraries.
PKG_CHECK_MODULES(XML, libxml++-2.6 >= 2.10.0 glibmm-2.4 >= 2.23.3)
AC_SUBST(XML_CFLAGS)
AC_SUBST(XML_LIBS)
PKG_CHECK_MODULES(SOCK, libSockets >= 2.3.9.2)
AC_SUBST(SOCK_CFLAGS)
AC_SUBST(SOCK_LIBS)
PKG_CHECK_MODULES(SQL, libsqlite3x  sqlite3 >= 3.6.0.0)
AC_SUBST(SQL_CFLAGS)
AC_SUBST(SQL_LIBS)
PKG_CHECK_MODULES(ARG, libArgV)
AC_SUBST(ARG_CFLAGS)
AC_SUBST(ARG_LIBS)
PKG_CHECK_MODULES(Z, zlib >= 1.2.3)
AC_SUBST(Z_CFLAGS)
AC_SUBST(Z_LIBS)
AC_CHECK_LIB([m], [main], [], [AC_MSG_ERROR([strange!! libm missing])])
AC_CHECK_LIB([l], [main], [], [AC_MSG_ERROR([libl missing])])

# Checks for header files.
AC_HEADER_STDC
AC_CHECK_HEADERS([cmath cstdio cstdlib cstring fstream iostream list map sstream stdexcept string vector stdlib.h string.h], [], [AC_MSG_ERROR([required headers not found])])

# Checks for typedefs, structures, and compiler characteristics.
AC_HEADER_STDBOOL
AC_C_INLINE
AC_TYPE_INT64_T
AC_TYPE_SIZE_T

# Checks for library functions.
AC_CHECK_FUNCS([unlink strncmp memset pow strlen strtod atof strtol atol sin cos asin sqrt], [], [AC_MSG_ERROR([required functions missing])])
AC_CHECK_FUNCS([strncasecmp], [], [AC_CHECK_FUNCS([strnicmp],[],[AC_MSG_ERROR([required functions missing])])])

AC_CONFIG_FILES([Makefile gnulib/Makefile])
AC_OUTPUT
