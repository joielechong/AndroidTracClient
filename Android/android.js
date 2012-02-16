if (window.innerWidth && window.innerWidth <= 480) { 1
    $(document).ready(function(){ 2
        $('#header ul').addClass('hide'); 3
        $('#header').append('<div class="leftButton" onclick="toggleMenu()">Menu</div>'); 4
    });
    function toggleMenu() { 
        $('#header ul').toggleClass('hide'); 5
        $('#header .leftButton').toggleClass('pressed'); 6
    }
}
