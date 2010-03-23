#                                               -*- Autoconf -*-
# Process this file with autoconf to produce a configure script.

AC_PREREQ([2.63])
AC_INIT([osm], [0.1])
AM_INIT_AUTOMAKE(osm,0.1)
AC_CONFIG_SRCDIR([main.cpp])
AC_CONFIG_HEADERS([config.h])
AC_LANG([C++])

# Checks for programs.
AC_PROG_CXX

# Checks for libraries.
PKG_CHECK_MODULES(DEPS, glibmm-2.4 libxml++-2.6 libsqlite3x  sqlite3 >= 3.6.0.0 ArgV)
AC_SUBST(DEPS_CFLAGS)
AC_SUBST(DEPS_LIBS)

# Checks for header files.
AC_HEADER_STDC
AC_CHECK_HEADERS(cmath cstdio cstdlib cstring fstream iostream list map sstream stdexcept string vector)

# Checks for typedefs, structures, and compiler characteristics.
AC_HEADER_STDBOOL
AC_C_INLINE
AC_TYPE_INT64_T

# Checks for library functions.
AC_CHECK_FUNCS([memset pow strlen strtod atof strtol atol sin cos asin sqrt strncasecmp strnicmp])

AC_CONFIG_FILES([Makefile])
AC_OUTPUT
