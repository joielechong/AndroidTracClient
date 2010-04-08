#                                               -*- Autoconf -*-
# Process this file with autoconf to produce a configure script.

AC_PREREQ(2.63)
AC_INIT([eisen],[0.1])
AM_INIT_AUTOMAKE(eisen,0.1)
AC_CONFIG_SRCDIR([eisen.cpp])
AC_CONFIG_HEADERS([config.h])
AC_LANG([C++])

# Checks for programs.
AC_PROG_CC
AC_PROG_CXX
AC_PROG_INSTALL

# Checks for libraries.
PKG_CHECK_MODULES(DEPS, libxml++-2.6 >= 2.10.0 glibmm-2.4 >= 2.23.3)
AC_SUBST(DEPS_CFLAGS)
AC_SUBST(DEPS_LIBS)

# Checks for header files.
AC_HEADER_STDC
AC_CHECK_HEADERS([cstdlib cstring iostream stdexcept string], [], [AC_MSG_ERROR([required headers not found])])

# Checks for typedefs, structures, and compiler characteristics.
AC_C_INLINE
AC_TYPE_SIZE_T

# Checks for library functions.

AC_CONFIG_FILES([Makefile])
AC_OUTPUT
