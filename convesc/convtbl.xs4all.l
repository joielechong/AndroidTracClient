%{
#define puts(a) fputs(a,yyout)
#define ploeg(f,p) fprintf(yyout,"<a href=\"ploeg.%s.html#%s\">",f,p);ECHO;puts("</a>");

%}
%%
"<"           puts("&lt;");
">"           puts("&gt;");
"&"           puts("&amp;");
"=46"         puts("F");
"=C0"         puts("&Agrave;");
"=C1"         puts("&Aacute;");
"=C2"         puts("&Acirc;");
"=C3"         puts("&Atilde;");
"=C4"         puts("&Auml;");
"=C5"         puts("&Aring;");
"=C6"         puts("&AElig;");
"=C7"         puts("&&Ccedil;");
"=C8"         puts("&Egrave;");
"=C9"         puts("&Eacute;");
"=CA"         puts("&Ecirc;");
"=CB"         puts("&Euml;");
"=CC"         puts("&Igrave;");
"=CD"         puts("&Iacute;");
"=CE"         puts("&Icirc;");
"=CF"         puts("&Iuml;");
"=D0"         puts("&ETH;");
"=D1"         puts("&Ntilde;");
"=D2"         puts("&Ograve;");
"=D3"         puts("&Oacute;");
"=D4"         puts("&Ocirc;");
"=D5"         puts("&Otilde;");
"=D6"         puts("&Ouml;");
"=D7"         puts("*");
"=D8"         puts("&Oslash;");
"=D9"         puts("&Ugrave;");
"=DA"         puts("&Uacute;");
"=DB"         puts("&Ucirc;");
"=DC"         puts("&Uuml;");
"=DD"         puts("&Yacute;");
"=DE"         puts("&THORN;");
"=DF"         puts("&szlig;");
"=E0"         puts("&agrave;");
"=E1"         puts("&aacute;");
"=E2"         puts("&acirc;");
"=E3"         puts("&atilde;");
"=E4"         puts("&auml;");
"=E5"         puts("&aring;");
"=E6"         puts("&aelig;");
"=E7"         puts("&ccedil;");
"=E8"         puts("&egrave;");
"=E9"         puts("&eacute;");
"=EA"         puts("&ecirc;");
"=EB"         puts("&euml;");
"=EC"         puts("&igrave;");
"=ED"         puts("&iacute;");
"=EE"         puts("&icirc;");
"=EF"         puts("&iuml;");
"=F0"         puts("&eth;");
"=F1"         puts("&ntilde;");
"=F2"         puts("&ograve;");
"=F3"         puts("&oacute;");
"=F4"         puts("&ocirc;");
"=F5"         puts("&otilde;");
"=F6"         puts("&ouml;");
"=F7"         puts("/");
"=F8"         puts("&oslash;");
"=F9"         puts("&ugrave;");
"=FA"         puts("&uacute;");
"=FB"         puts("&ucirc;");
"=FC"         puts("&uuml;");
"=FD"         puts("&yacute;");
"=FE"         puts("&thorn;");
"=FF"         puts("&yuml;");
"=\n"         puts("");
",,"          puts("<cite>");
"''"          puts("</cite>");

"all s.t."    puts("alle z.t.");
"s.t."        puts("z.t.");

"Czech Republic" puts("Tsjechi&euml;");
"Germany"     puts("Duitsland");
"(Ger)"       puts("(Dui)");
"France"      puts("Frankrijk");
"Italy"       puts("Itali&euml;");
"Belgium"     puts("Belgi&euml;");
"Spain"       puts("Spanje");
"Russia"      puts("Rusland");
"Estonia"     puts("Estland");
"Austria"     puts("Oostenrijk");
"Ukraine"     puts("Oekraine");
"United States" puts("Verenigde Staten");
"Switzerland" puts("Zwitserland");
"Denmark"     puts("Denemarken");
"(Cze)"       puts("(Tsj)");
"(Lat)"       puts("(Let)");
"(Sch)"       puts("(Zwi)");

"Eindklassement:"       puts("<h4>Eindklassement</h4>");
"Eind klassement:"      puts("<h4>Eindklassement</h4>");
"Algemeen klassement:"  puts("<h4>Algemeen klassement</h4>");
"Klassement:"           puts("<h4>Klassement</h4>");
"Final Overall:"        puts("<h4>Eindklassement</h4>");
"Overall:"              puts("<h4>Klassement</h4>");
"Overall standing"      puts("<h4>Algemeen klassement</h4>");


"tvm"                         ploeg("nl","tvm");
"rabobank"                    ploeg("nl","rabobank");
"rabo"                        ploeg("nl","rabobank");
"foreldorado-golff"           ploeg("nl","foreldorado");
"foreldorado golff"           ploeg("nl","foreldorado");
"foreldorado"                 ploeg("nl","foreldorado");

"comptel data"                ploeg("usa","comptel");
"comptel-data"                ploeg("usa","comptel");
"comptel"                     ploeg("usa","comptel");
"saturn"                      ploeg("usa","saturn");
"usa postal"                  ploeg("usa","uspostal");
"us postal"                   ploeg("usa","uspostal");
"postal"                      ploeg("usa","uspostal");
"plymouth"                    ploeg("usa","plymouth");

"collstrop"                   ploeg("b","collstrop");
"lotto"                       ploeg("b","lotto");
"vlaanderen 2002"             ploeg("b","vlaand");
"palmans"                     ploeg("b","palmans");
"saxon"                       ploeg("b","saxon");
"cedico"                      ploeg("b","cedico");
"IPSO"                        ploeg("b","asfra");
"rdm asfra"                   ploeg("b","rdm");
"rdm-asfra"                   ploeg("b","rdm");
"asfra"                       ploeg("b","rdm");
"rdm"                         ploeg("b","rdm");

"krka telekom"                ploeg("slo","krka");
"krka"                        ploeg("slo","krka");

"telekom"                     ploeg("d","telekom");
"team nurnberg"               ploeg("d","nb");
"nurnberger"                  ploeg("d","nb");
"n&uuml;rnberger"             ploeg("d","nb");
"n=FCrnberger"                ploeg("d","nb");
"E-Plus Service"              ploeg("d","eplus");
"E-Plus"                      ploeg("d","eplus");
"Team EC-Bayer"               ploeg("d","ec");
"Team EC Bayer"               ploeg("d","ec");
"EC-Bayer"                    ploeg("d","ec");
"Bayer"                       ploeg("d","ec");
"PSV-Team Cologne"            ploeg("d","psv");
"Team Cologne"                ploeg("d","psv");

"aubervilliers 93"            ploeg("f","aubervilliers");
"aubervilliers"               ploeg("f","aubervilliers");
"big mat auber 93"            ploeg("f","aubervilliers");
"big mat-auber 93"            ploeg("f","aubervilliers");
"big mat"                     ploeg("f","aubervilliers");
"gan"                         ploeg("f","gan");
"casino"                      ploeg("f","casino");
"Mutuelle de Seine-et-Marne"  ploeg("f","mutuelle");
"mutuelle"                    ploeg("f","mutuelle");
"francaise des jeux"          ploeg("f","franjeux");
"=46rancaise des jeux"        ploeg("f","franjeux");
"festina"                     ploeg("f","festina");
"=46estina-lotus"             ploeg("f","festina");
"=46estina lotus"             ploeg("f","festina");
"=46estina"                   ploeg("f","festina");
"cofidis"                     ploeg("f","cofidis");

"mapei GB"                    ploeg("i","mapei");
"mapei-GB"                    ploeg("i","mapei");
"aki"                         ploeg("i","aki");
"asics"                       ploeg("i","asics");
"mapei"                       ploeg("i","mapei");
"polti"                       ploeg("i","polti");
"team polti"                  ploeg("i","polti");
"mercato uno"                 ploeg("sm","mercato");
"saeco"                       ploeg("sm","saeco");
"amore&vita"                  ploeg("i","amore");
"amore e vita"                ploeg("i","amore");
"amore"                       ploeg("i","amore");
"brescialat"                  ploeg("i","brescialat");
"scrigno"                     ploeg("i","scrigno");
"mg"                          ploeg("i","mg");
"refin"                       ploeg("i","refin");
"cantina tollo"               ploeg("i","cantina");
"tollo"                       ploeg("i","cantina");
"cantina"                     ploeg("i","cantina");

"batik"                       ploeg("mc","batik");
"mroz"                        ploeg("pl","mroz");

"euskadi"                     ploeg("e","euskadi");
"once"                        ploeg("e","once");
"kelme-artiach"               ploeg("e","kelme");
"artiach"                     ploeg("e","kelme");
"kelme"                       ploeg("e","kelme");
"banesto"                     ploeg("e","banesto");
"deportpublic"                ploeg("e","deport");
"deport"                      ploeg("e","deport");

"master"                      ploeg("ch","master");

"maia"                        ploeg("p","maia");
"boavista"                    ploeg("p","boavista");
"al aluminios"                ploeg("p","al");
"al-aluminios"                ploeg("p","al");
"la aluminios"                ploeg("p","al");
"la-aluminios"                ploeg("p","al");
"LA Aluminio"                 ploeg("p","al");
"aluminios"                   ploeg("p","al");
"tavira"                      ploeg("p","tavira");

"glacial"                     ploeg("co","glacial");
"kross montiniari"            ploeg("co","kross");
"kross-montiniari"            ploeg("co","kross");
"kross"                       ploeg("co","kross");
"montiniari"                  ploeg("co","kross");

"\n\n"                        fprintf(yyout,"<p>\n\n");

[0-9]".\n"                    ECHO;
".\n"                         fprintf(yyout,".<br>\n");

[a-zA-Z]+                     ECHO;

.                             ECHO;
%%
