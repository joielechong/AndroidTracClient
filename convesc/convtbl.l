%{
#define puts(a) fputs(a,yyout)
#define ploeg_oud(f,p) fprintf(yyout,"<a href=\"ploeg.%s.html#%s\">",f,p);ECHO;puts("</a>");

#define ploeg(f,p) fprintf(yyout,"<a href=\"http://www.uci.ch/english/road/teams/%s.htm\">",p);ECHO;puts("</a>");

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
"  Stage "              puts("<h4>Etappe ");
"  Prologue "           puts("<h4>Proloog ");
"km:\n"                 puts("km</h4>\n");

"acceptcard pro cycling"      ploeg("dk","99acc");
"acceptcard"                  ploeg("dk","99acc");

"agro-adler-brandenburg"      ploeg("d","99aab");
"agro-adler"                  ploeg("d","99aab");
"agro adler"                  ploeg("d","99aab");
"brandenburg"                 ploeg("d","99aab");

"amica-chips"                 ploeg("i","99ami");
"amica chips"                 ploeg("i","99ami");
"amica"                       ploeg("i","99ami");

"amore&vita"                  ploeg("gb","99amo");
"amore e vita"                ploeg("gb","99amo");
"amore"                       ploeg("gb","99amo");

"ballan-alessio"              ploeg("i","99bal");
"ballan alessio"              ploeg("i","99bal");
"ballan"                      ploeg("i","99bal");

"banesto"                     ploeg("e","99ban");

"batavus-bankgiroloterij"     ploeg("nl","99bbt");
"batavus bankgiroloterij"     ploeg("nl","99bbt");
"batavus"                     ploeg("nl","99bbt");

"big mat auber 93"            ploeg("f","99big");
"big mat"                     ploeg("f","99big");
"bigmat auber 93"             ploeg("f","99big");
"bigmat"                      ploeg("f","99big");

"cantina tollo-alexia alluminio" ploeg("i","99cta");
"cantina tollo-alexia"        ploeg("i","99cta");
"cantina tollo"               ploeg("i","99cta");
"tollo"                       ploeg("i","99cta");
"cantina"                     ploeg("i","99cta");

"casino"                      ploeg("f","99cso");

"cofidis"                     ploeg("f","99cof");

"collstrop"                   ploeg("b","99cos");

"credit agricole"             ploeg("f","99ca");

"die continentale-olympia"    ploeg("d","99con");
"continentale-olympia"        ploeg("d","99con");
"continentale"                ploeg("d","99con");

"team ec / bayer"             ploeg("d","99ecb");
"team ec/bayer"               ploeg("d","99ecb");

"euskaltel-euskadi"           ploeg("e","99eus");
"euskaltel euskadi"           ploeg("e","99eus");
"euskaltel"                   ploeg("e","99eus");
"euskadi"                     ploeg("e","99eus");

"festina-lotus"               ploeg("f","99fes");
"festina lotus"               ploeg("f","99fes");
"festina"                     ploeg("f","99fes");
"=46estina-lotus"             ploeg("f","99fes");
"=46estina lotus"             ploeg("f","99fes");
"=46estina"                   ploeg("f","99fes");
"=66estina-lotus"             ploeg("f","99fes");
"=66estina lotus"             ploeg("f","99fes");
"=66estina"                   ploeg("f","99fes");

"fuenlabrada"                 ploeg("e","99fue");
"=46uenlabrada"               ploeg("e","99fue");
"=66uenlabrada"               ploeg("e","99fue");

"home market"                 ploeg("b","99hom");
"ville de charleroi"          ploeg("b","99hom");

"ipso-euroclean"              ploeg("b","99ips");
"ipso euroclean"              ploeg("b","99ips");
"ipso"                        ploeg("b","99ips");

"kelme-costa blanca"          ploeg("e","99kel");
"kelme"                       ploeg("e","99kel");

"krka telekom slovenije"      ploeg("slo","99krk");
"krka telekom"                ploeg("slo","99krk");
"krka-telekom slovenije"      ploeg("slo","99krk");
"krka-telekom"                ploeg("slo","99krk");
"krka"                        ploeg("slo","99krk");

"selle italia"                ploeg("co","99sel");

"la francaise des jeux"       ploeg("f","99fdj");
"francaise des jeux"          ploeg("f","99fdj");
"=46rancaise des jeux"        ploeg("f","99fdj");

"lampre-daikin"               ploeg("i","99lam");
"lampre daikin"               ploeg("i","99lam");
"lampre"                      ploeg("i","99lam");

"la-pecol"                    ploeg("p","99lap");
"la/pecol"                    ploeg("p","99lap");
"la pecol"                    ploeg("p","99lap");

"lotto-mobistar"              ploeg("b","99lot");
"lotto mobistar"              ploeg("b","99lot");
"lotto"                       ploeg("b","99lot");

"linda mccartney racing team" ploeg("gb","99lin");
"linda mccartney"             ploeg("gb","99lin");
"mccartney"                   ploeg("gb","99lin");

"liquigas-pata"               ploeg("i","99liq");
"liquigas"                    ploeg("i","99liq");

"maia/cin"                    ploeg("p","99mai");
"maia-cin"                    ploeg("p","99mai");
"maia cin"                    ploeg("p","99mai");
"maia"                        ploeg("p","99mai");

"mapei-quick step"            ploeg("i","99map");
"mapei-bricobi"               ploeg("i","99map");
"mapei bricobi"               ploeg("i","99map");
"mapei"                       ploeg("i","99map");

"mercatone uno-bianchi"       ploeg("i","99mer");
"mercatone uno bianchi"       ploeg("i","99mer");
"mercatone uno"               ploeg("i","99mer");

"mercury cycling team"        ploeg("usa","99mct");
"mercury"                     ploeg("usa","99mct");

"mobilvetta design-northwave" ploeg("i","99mdn");
"mobilvetta-northwave"        ploeg("i","99mdn");
"mobilvetta"                  ploeg("i","99mdn");

"mroz"                        ploeg("pl","99mro");

"navigare-gaerne"             ploeg("i","99nav");
"navigare gaerne"             ploeg("i","99nav");
"navigare"                    ploeg("i","99nav");

"navigators cycling team"     ploeg("usa","99nvr");
"navigators"                  ploeg("usa","99nvr");

"once-deutsche bank"          ploeg("e","99onc");
"once"                        ploeg("e","99onc");

"palmans-ideal"               ploeg("b","99pal");
"palmans ideal"               ploeg("b","99pal");
"palmans"                     ploeg("b","99pal");

"porta da ravessa-milaneza"   ploeg("p","99prm");
"porta da ravessa"            ploeg("p","99prm");

"post swiss team"             ploeg("ch","99pos");
"post swiss"                  ploeg("ch","99pos");

"rabobank"                    ploeg("nl","99rab");
"rabo"                        ploeg("nl","99rab");

"riso scotti-vinavil"         ploeg("i","99ris");
"riso scotti vinavil"         ploeg("i","99ris");
"riso scotti"                 ploeg("i","99ris");

"saeco-cannondale"            ploeg("i","99sae");
"saeco"                       ploeg("i","99sae");

"saturn cycling team"         ploeg("usa","99sat");
"saturn"                      ploeg("usa","99sat");

"spar-rdm"                    ploeg("b","99spa");
"spar rdm"                    ploeg("b","99spa");
"spar"                        ploeg("b","99spa");

"sport lisboa e benfica"      ploeg("p","99slb");
"benfica"                     ploeg("p","99slb");
"sport lisboa"                ploeg("p","99slb");

"tvm-farm frites"             ploeg("nl","99tvm");
"tvm farm frites"             ploeg("nl","99tvm");
"farm frites"                 ploeg("nl","99tvm");
"tvm"                         ploeg("nl","99tvm");

"team chicky world"           ploeg("dk","99tcw");
"chicky world"                ploeg("dk","99tcw");

"team cologne"                ploeg("d","99col");
"cologne"                     ploeg("d","99col");

"team ericsson villiger"      ploeg("ch","99eri");
"team ericsson"               ploeg("ch","99eri");
"team ericsson-villiger"      ploeg("ch","99eri");
"ericsson villiger"           ploeg("ch","99eri");
"ericsson-villiger"           ploeg("ch","99eri");
"ericsson"                    ploeg("ch","99eri");

"team deutsche telekom"       ploeg("d","99tel");
"deutsche telekom"            ploeg("d","99tel");
"telekom"                     ploeg("d","99tel");

"team gerolsteiner"           ploeg("d","99gst");
"gerolsteiner"                ploeg("d","99gst");

"team home-jack&jones"        ploeg("dk","99hjj");
"team home jack&jones"        ploeg("dk","99hjj");
"jack&jones"                  ploeg("dk","99hjj");

"team nurnberger"             ploeg("d","99nur");
"team n=FCrnberger"           ploeg("d","99nur");
"nurnberger"                  ploeg("d","99nur");
"n&uuml;rnberger"             ploeg("d","99nur");
"n=FCrnberger"                ploeg("d","99nur");

"team polti"                  ploeg("i","99plt");
"polti"                       ploeg("i","99plt");

"tonissteiner-colnago"        ploeg("b","99ton");
"tonissteiner colnago"        ploeg("b","99ton");
"tonissteiner"                ploeg("b","99ton");
  
"usa postal"                  ploeg("usa","99usp");
"us postal service"           ploeg("usa","99usp");
"us postal"                   ploeg("usa","99usp");
"postal"                      ploeg("usa","99usp");

"vini caldirola-sidermec"     ploeg("i","99vin");
"vini caldirola sidermec"     ploeg("i","99vin");
"vini caldirola"              ploeg("i","99vin");

"vitalicio siguros-grupo generali" ploeg("e","99vit");
"vitalicio seguros"           ploeg("e","99vit");
"vitalicio siguros"           ploeg("e","99vit");
"vitalicio"                   ploeg("e","99vit");

"vlaanderen 2002"             ploeg("b","99vla");

"wustenrot-zvvz"              ploeg("cz","wus");
"wustenrot zvvz"              ploeg("cz","wus");
"wustenrot"                   ploeg("cz","wus");
"zvvz"                        ploeg("cz","wus");

"\n\n"                        fprintf(yyout,"\n<p>\n");

[0-9]".\n"                    ECHO;
".\n"                         fprintf(yyout,".\n<br>\n");

[a-zA-Z]+                     ECHO;

.                             ECHO;
%%
