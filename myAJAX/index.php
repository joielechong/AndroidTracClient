<html>
<head>
<title>myAJAX simple tests</title>
<script type="text/javascript" language="javascript" src="js/myajax.php"></script>
<script type="text/javascript" language="javascript">
/*
//myAJAX - passing parameters through an object example 
var aParams = new Object();
aParams.form = 'fileForm';
aParams.onStartFunction = function() { self.document.getElementById('id1').innerHTML = '<h1>Loading...</h1>'; $toggleVisibility($('loading'));  };
aParams.onReadyFunction = function(msg) { self.document.getElementById('id1').innerHTML = '<h1>DONE.</h1>'+msg; $toggleVisibility($('loading')); };
var a = new myAJAX(aParams);
*/

//myAJAX - passing parameters through an object example 
var a = new myAJAX({ urlParameters: 'fileForm', onStartFunction: function() { $show($('loading')); }, onReadyFunction: function(msg) { $hide($('loading')); }, enableMultiplyingFileInputs: true, enableStyleEffects: true, InnerHtmlId: "id0" });


var b=new myAJAX();

function ProgressFunction(msg,leng)
{ if(leng) window.status=leng; }

function ErrFunction(msg,code)
{ alert(code+'\r\n'+msg); }

//myAJAX - passing parameters through object's properties and setting an autoupdater example
function obj1()
{
b.url='called/foo.php'; 
b.urlParameters='message1=value1&message2=value2'; 
b.onProgressFunction=ProgressFunction;
b.onErrorFunction=ErrFunction; 
b.refreshInterval=2000; 
b.InnerHtmlId="id1";
//b.ajaxMethod = myAJAX.constants.ajaxMethod.iFrame; /* force using IFRAME based method */
b.sendRequest();
}

//myAJAX - passing parameters through object's properties ang parsing xml response example
function xmlRq()
{
var x = new myAJAX('called/xml.xml');
x.method = 'POST';
x.onReadyFunction = myXMLtest;
//x.ajaxMethod = myAJAX.constants.ajaxMethod.iFrame; /* force using IFRAME based method */
x.sendRequest();
}


function myXMLtest(xmlDoc)
{
var usr=xmlDoc.nodesByTagName('name');
for(i=0;i<usr.length;i++) if(!confirm('value='+xmlDoc.nodeValue(usr[i]) + '\r\nid='+xmlDoc.nodeAttribute(usr[i],'id'))) break;
}






function draggable()
{
var img = document.$createElement('img', { src: 'img/tux.png' }, self.document.body );
myAJAX.draggable(img);
}
</script>
<style>
#loading      { 
                position: absolute;
                top: 12px;
                right: 12px;
                visibility: hidden;
}

#getFirefox      { 
                position: absolute;
                bottom: 12px;
                left: 12px;
}

#sf              { 
                position: absolute;
                bottom: 12px;
                right: 12px;
}

select, textarea, input.myinput { 
                color: #000000; 
                background-color: #91B1E7; 
                border: 1px #000000 solid; 
}

select:focus, textarea:focus, input.myinput:focus, input[type="file"]:focus { 
                background-color: #CC0000; 
                font-weight: bold;

}

fieldset {
                width: 550px; 
                padding: 4px,4px,4px,4px;
                background-color: #ECEBE6; 
                border-width: 1px; 
                border-color: #000000; 
                border-style: solid; 
                
}

h2        {
                color: #FFFFFF;
}

body     { 
                color: #000000; 
                background: url(img/bg.gif); 
                background-repeat: repeat-x;
}

</style>

</head>
<body>
<img src="img/loading.gif" width="32" height="32" border="0" id="loading">
<h2 id="title">myAJAX <script type="text/javascript">document.write(myAJAX.version); </script> simple tests</h2>
<fieldset>
<form enctype="multipart/form-data" action="called/upload.php" method="get" id="fileForm" target="_blank" onsubmit="return a.sendRequest();">
<table cellpadding="4" cellspacing="2" border="0" width="100%">
        <tr valign="middle">
                <td align="left" width="50%">
                        File:
                </td>
                <td align="right" width="50%">
                           <input type="hidden" name="MAX_FILE_SIZE" value="30000000" > <input name="userfile[]" type="file" class="myinput">
                </td>

        </tr>        
        <tr valign="middle">
                <td align="left" width="50%">
                        Select:
                </td>
                <td align="right" width="50%">
                           <select name="select[]" id="mySelect" class="myinput">
                                <option></option>
                                <option value="Selected Value #1" selected>Selected Value #1</option>
                                <option value="Selected Value #2">Selected Value #2</option>
                                <option value="Selected Value #3">Selected Value #3</option>
                           </select>
                </td>

        </tr>        
        <tr valign="middle">
                <td align="left" width="50%">
                        Text:
                </td>
                <td align="right" width="50%">
                           <input name="text[]"  class="myinput">
                </td>

        </tr>        
        <tr valign="middle">
                <td align="left" width="50%">
                        Text area:
                </td>
                <td align="right" width="50%">
                           <textarea name="textArea"></textarea>
                </td>

        </tr>        
        <tr valign="middle">
                <td align="left" width="50%">
                        Radio:
                </td>
                <td align="right" width="50%">
                           Radio Value 1: <input type="radio" name="radio[]" value="radio1" class="myinput" >&nbsp;Radio Value 2: <input type="radio" name="radio[]" value="radio2" class="myinput" >
                </td>

        </tr>        

        <tr valign="middle">
                <td align="left" width="50%">
                        Password:
                </td>
                <td align="right" width="50%">
                           <input name="passwod[]" type="password"  class="myinput">
                </td>

        </tr>        

        <tr valign="middle" align="left">
                <td colspan="2">
                        <input type="submit" name="send" value="Send&nbsp;">&nbsp;<input type="reset" value="Reset">
                </td>
        </tr>        
</table>
</form>
</fieldset>

<br>

<br>
<input type="button" name="button" value="start autoupdater" onClick="obj1()">
<input type="button" name="button" value="stop autoupdater" onClick="b.abortRequest();">
<input type="button" name="button" value="&nbsp;&nbsp;xml&nbsp;&nbsp;" onClick="xmlRq();">
<br>
<input type="button" value="toggleOpacity" onclick="$('sf').$toggleOpacity()">
<input type="button" value="draggable" onclick="draggable()">
<br>
<br><br>
<div id="id1">myAJAX</div><br>
<div id="id0">myAJAX</div>
<br><br>
<a href="http://www.mozilla.com/en-US/firefox/" target="_blank" id="getFirefox"><img src="img/getfirefox_88x31.png" width="88" height="31" border="0"></a>
<a href="http://sourceforge.net/" target="_blank" id="sf"><img src="img/sflogo.png" width="127" height="35" border="0" id="sflogo"></a>
<script type="text/javascript">
//document.onmousemove=function (event){ var xy = $getMousePosition(event); self.status = "Mouse X:"+xy.x + " Mouse Y:"+xy.y; };
//document.onmousemove = function(event) { event = event || window.event; self.status = event.type; }
//document.onmousedrag = function(event) { event = event || window.event; self.status = event.type; }
</script>
</body>
</html>
