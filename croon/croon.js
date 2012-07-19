function wisdiv(elem) {
    "use strict";
    var div = document.getElementById(elem);
    div.innerHTML = '<p></p>';
    return true;
}
function wisdivs(elems) {
    "use strict";
    var i;
    for (i = 0; i < elems.length; i++) {
        wisdiv(elems[i]);
    }
}
function loaddiv(elem) {
    "use strict";
    var div = document.getElementById(elem);
    div.innerHTML = '<p>Loading.......</p>';
    return true;
}
function loaddivs(elems) {
    "use strict";
    var i;
    for (i = 0; i < elems.length; i++) {
        loaddiv(elems[i]);
    }
}
function wisveld(veld) {
    "use strict";
    document.getElementById(veld).value = '';
    return true;
}
function wisvelden(velden) {
    "use strict";
    var i;
    for (i = 0; i < velden.length; i++) {
        wisveld(velden[i]);
    }
}

function div_en_func(data) {
  var dt=this.target;
  if (dt.constructor != Array) { dt=[dt]; }
  var div = document.getElementById(dt[1]);
  var func = dt[2];
  if (div != null) {
    if (div.type =='text' || div.type=='textarea' || div.type=='hidden' ) {
      div.value=data;
    } else if (div.type =='checkbox') {
      div.checked=data;
    } else {
      div.innerHTML = data;
    }
  }
  if (func != null) {
    func.apply();
  }
}

function eisen_detail_ind() {
   var sel = document.getElementById('eisselect');
   if (sel.options.length > 0) {
     sel.value = sel.options[0].value;
     eisen_detail();
   }
}

function eisen_zoek_eiscode() {
    "use strict";
    wisdivs(['d_eisvmx','d_eisdet','d_eisprop','d_vmx']);
    wisvelden(['searchtermdi', 'searchtermtekst', 'searchtermlokatie', 'searchtermfasering']);
    searchcode(['searchterm'], [div_en_func,'d_inpeis',eisen_detail_ind], 'POST');
    return true;
}
function eisen_zoek_di() {
    "use strict";
    wisdivs(['d_eisvmx','d_eisdet','d_eisprop','d_vmx']);
    wisvelden(['searchterm', 'searchtermtekst', 'searchtermlokatie', 'searchtermfasering']);
    searchdi(['searchtermdi'], [div_en_func,'d_inpeis',eisen_detail_ind], 'POST');
    return true;
}
function eisen_vmx_di() {
    "use strict";
    wisdivs(['d_eisdet','d_eisprop','d_vmx']);
    loaddiv('d_eisvmx');
    vmxdi(['searchtermdi'], ['d_eisvmx'], 'POST');
    return true;
}
function eisen_zoek_eistekst() {
    "use strict";
    wisdivs(['d_eisvmx','d_eisdet','d_eisprop','d_vmx']);
    wisvelden(['searchtermdi', 'searchterm', 'searchtermlokatie', 'searchtermfasering']);
    searchtekst(['searchtermtekst'], [div_en_func,'d_inpeis',eisen_detail_ind], 'POST');
    return true;
}
function eisen_vmx_eistekst() {
    "use strict";
    wisdivs(['d_eisdet','d_eisprop','d_vmx']);
    loaddiv('d_eisvmx');
    vmxeistekst(['searchtermtekst'], ['d_eisvmx'], 'POST');
    return true;
}
function eisen_zoek_fase() {
    "use strict";
    wisdivs(['d_eisvmx','d_eisdet','d_eisprop','d_vmx']);
    wisvelden(['searchtermdi', 'searchterm', 'searchtermlokatie', 'searchtermtekst']);
    searchfasering(['searchtermfasering'], [div_en_func,'d_inpeis',eisen_detail_ind], 'POST');
    return true;
}
function eisen_vmx_fase() {
    "use strict";
    loaddiv('d_vmx');
    vmxfasering(['searchtermfasering'], ['d_vmx'], 'POST');
    return true;
}
function eisen_zoek_lokatie() {
    "use strict";
    wisdivs(['d_eisvmx','d_eisdet','d_eisprop','d_vmx']);
    wisvelden(['searchtermdi', 'searchterm', 'searchtermtekst', 'searchtermfasering']);
    searchlokatie(['searchtermlokatie'], [div_en_func,'d_inpeis',eisen_detail_ind], 'POST');
    return true;
}
function eisen_vmx_lokatie() {
    "use strict";
    loaddiv('d_vmx');
    vmxlokatie(['searchtermlokatie'], ['d_vmx'], 'POST'); 
    return true;
}
function eisen_detail() {
    "use strict";
    wisdivs(['d_vmx']);
    loaddivs(['d_eisvmx','d_eisdet','d_eisprop']);
    details(['eisselect'], ['d_eisprop'], 'POST');
    didetails(['eisselect'], ['d_eisdet'], 'POST'); 
    vmxdetails(['eisselect'], ['d_eisvmx'], 'POST'); 
    return true;
}

function info_fases_add() {
    "use strict";
    wisdivs(['d_faselocprot', 'd_faselocproteis']);
    loadaddfaseloc(['faselocval'], ['d_addfaseloc'], 'POST');
    return true;
}
function info_fases_locs_add() {
    "use strict";
    wisdiv('d_faselocproteis');
    loadaddfaselocprot(['faselocval', 'faselocprotval'], ['d_addfaselocprot'], 'POST');
    return true;
}
function loadaddfaseloc_add() {
    "use strict";
	insfaseloc(['addfaselocsel', 'addfaselocrem', 'faselocval'], ['d_faseloc'], 'POST');
	return true;
}
function loadaddfaselocprot_add() {
    "use strict";
	insfaselocprot(['addfaselocprotsel', 'faselocval', 'faselocprotval'], ['d_faselocprot'], 'POST');
	return true;
}
function loadaddfaselocreport_add() {
    "use strict";
	insfaselocprot(['addfaselocreportsel', 'locid'], ['d_faselocreport'], 'POST');
	return true;
}

function info_locprot_add() {
    "use strict";
    loadaddlocprot(['locid'], ['d_addlocprot'], 'POST');
    return true;
}
function loadaddlocprot_add() {
    "use strict";
    inslocprot(['addlocprotsel', 'addlocprotfase', 'locid'], ['d_loc_prot'], 'POST');
    return true;
}
function info_locreport_add() {
    "use strict";
    loadaddlocreport(['locid'], ['d_addlocreport'], 'POST');
    return true;
}
function loadaddlocreport_add() {
    "use strict";
    inslocreport(['addlocreportsel', 'addlocreportfase', 'locid'], ['d_loc_report'], 'POST');
    return true;
}
function info_locfase_add() {
    "use strict";
    loadaddlocfase(['locid'], ['d_addlocfase'], 'POST');
    return true;
}
function loadaddlocfase_add() {
    "use strict";
	inslocfase(['addlocfasesel', 'addlocfaserem', 'locid'], ['d_loc_fase'], 'POST');
	return true;
}
function laad_lokaties_add() {
    "use strict";
    addlocatie(['searchterm'], ['d_addlocatie'], 'POST');
    return true;
}
function loadaddlocatie() {
    "use strict";
    inslocatie(['addlocatie_id', 'addlocatie_beschr'], ['d_loc'], 'POST');
    return true;
}

function info_report_fase_add() {
    "use strict";
    loadaddreportfase(['reportid'], ['d_addreportfase'], 'POST');
    return true;
}
function loadaddreportfase_add() {
    "use strict";
	insreportfase(['addreportfasesel', 'reportid'], ['d_report_fase'], 'POST');
	return true;
}
function info_report_loc_add() {
    "use strict";
    loadaddreportloc(['reportid'], ['d_addreportloc'], 'POST');
    return true;
}
function loadaddreportloc_add() {
    "use strict";
	insreportloc(['addreportlocsel', 'addreportlocfase', 'reportid'], ['d_report_loc'], 'POST');
	return true;
}
function info_report_prot_add() {
    "use strict";
    loadaddreportprot(['reportid'], ['d_addreportprot'], 'POST');
    return true;
}
function loadaddreportprot_add() {
    "use strict";
	insreportprot(['addreportprotsel', 'reportid'], ['d_report_prot'], 'POST');
	return true;
}
function info_report_status_mod() {
    "use strict";
    loadreportstatus(['reportid'], ['d_report_status'], 'POST');
    return true;
}
function loadmodreportstatus_mod() {
    "use strict";
    modreportstatus(['modreportstatussel', 'reportid'], ['d_report_info'], 'POST');
    return true;
}
function info_report_eisen_add() {
    "use strict";
    loadaddreporteis(['reportid'], ['d_addreport_eis'], 'POST');
    return true;
}

function info_prot_status_mod() {
    "use strict";
    loadprotstatus(['protocolid'], ['d_protocol_status'], 'POST');
    return true;
}
function loadmodprotstatus_mod() {
    "use strict";
    modprotstatus(['modprotstatussel', 'protocolid'], ['d_prot_info'], 'POST');
    return true;
}
function info_prot_loc_add() {
    "use strict";
    loadaddprotloc(['protocolid'], ['d_addprotloc'], 'POST');
    return true;
}
function loadaddprotloc_add() {
    "use strict";
	insprotloc(['addprotlocsel', 'addprotlocfase', 'protocolid'], ['d_prot_loc'], 'POST');
	return true;
}
function info_prot_loc_fase_add() {
    "use strict";
	loadaddprotlocfase(['protocolid', 'addprotlocsel'], ['d_addprotloc_fase'], 'POST');
	return true;
}


function toonander(eis) {
    "use strict";
    document.getElementById('nieuweeis').value = eis;
    details(['nieuweeis'], ['d_eisprop'], 'POST'); 
    didetails(['nieuweeis'], ['d_eisdet'], 'POST'); 
    return true;
}

function buttonalert(event,key,kolom,rij) {
    var button;
    if (event.which == null)
        button= (event.button < 2) ? "LEFT" :
             ((event.button == 4) ? "MIDDLE" : "RIGHT");
    else
        button= (event.which < 2) ? "LEFT" :
             ((event.which == 2) ? "MIDDLE" : "RIGHT");
    alert(button);
    dont(event);
}
function voerprotoin(event,key,kolom,rij) {
    window.open('index.cgi?cmd=voerprotoin&eis='+key+'&di='+rij[0],'testvenster','width=800,height=600,scrollbars=yes,location=no');
    dont(event);
}
function dont(event)
{
    if (event.preventDefault)
        event.preventDefault();
    else
        event.returnValue= false;
     return false;
}

function eisen_proto(eis,di) {
  document.getElementById('vprotoeis_eis').value=eis;
  document.getElementById('vprotoeis_di').value=di;
  store_eisen_proto(['vprotoeis_eis','vprotoeis_di','veisdiprot'],['d_vproteis'],'POST');
  return  true;
}

function altvmx(eis,di,fase) {
  document.getElementById('altvmx_eis').value=eis;
  document.getElementById('altvmx_di').value=di;
  document.getElementById('altvmx_fase').value=fase;
  do_altvmx(['altvmx_eis','altvmx_di','altvmx_fase'],['d_'+eis+'_'+di+'_'+fase],'POST');
  return true;
}
function addvmxclass(eis,di,fase) {
  var n = document.getElementById('d_'+eis+'_'+di+'_'+fase);
  if (n) {
    if (! n.className.match('locked')) {
      n.className += (n.className?' locked':'locked');
    }
  }
  return true;
}
function delvmxclass(eis,di,fase) {
  var n = document.getElementById('d_'+eis+'_'+di+'_'+fase);
  var cl = 'locked';
  if (n) {
    var rep=n.className.match(' '+cl)?' '+cl:cl;
    n.className=n.className.replace(rep,'');
  }
  return true;
}
function altvmxlc(eis,di,lock) {
   altvmx(eis,di,'lc');
  if (lock == 0) {
    addvmxclass(eis,di,'ov');
    addvmxclass(eis,di,'ke');
    addvmxclass(eis,di,'bp');
    addvmxclass(eis,di,'fat');
    addvmxclass(eis,di,'ifat');
    addvmxclass(eis,di,'sat');
    addvmxclass(eis,di,'isat');
    addvmxclass(eis,di,'sit');
    addvmxclass(eis,di,'in');
  } else {
    delvmxclass(eis,di,'ov');
    delvmxclass(eis,di,'ke');
    delvmxclass(eis,di,'bp');
    delvmxclass(eis,di,'fat');
    delvmxclass(eis,di,'ifat');
    delvmxclass(eis,di,'sat');
    delvmxclass(eis,di,'isat');
    delvmxclass(eis,di,'sit');
    delvmxclass(eis,di,'in');
  }
  return true;
}

function setoption() {
    var sel = document.getElementById(arguments[0]);
    sel.options.length = 0;
    var l=arguments.length-1;
    for (i=0;i<l;i++) {
        sel.options[sel.options.length]=new Option(arguments[i+1],arguments[i+1],false,false);
    }
}
function setoption2() {
    var sel = document.getElementById(arguments[0]);
    sel.options.length = 0;
    var l=arguments.length-1;
    for (i=0;i<l;i+=2) {
        sel.options[sel.options.length]=new Option(arguments[i+1]+' - '+arguments[i+2],arguments[i+1],false,false);
    }
}
function croon_initialize() {
	laad_searchdi(['searchterm'],[setoption2],'POST');
	laad_searchfasering(['searchterm'],[setoption2],'POST');
	laad_searchlocatie(['searchterm'],[setoption2],'POST');
	initTabs('dhtmlgoodies_tabView1',Array('Eisen','Locaties','Componenten','Protocollen','Rapporten','Faseringen','Overig'),0,'1600','',Array(false,false,false,false,false,false));
	laad_lokaties(['searchterm'],['d_loc'],'POST');
	laad_protocollen(['searchterm'],['d_proto'],'POST');
	laad_rapporten(['searchterm'],['d_report'],'POST');
	laad_fasering(['searchterm'],['d_fase'],'POST');
}
