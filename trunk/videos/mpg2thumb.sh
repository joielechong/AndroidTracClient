for i in /mnt/hdd1/JVC-20060819/SD_VIDEO/PRG*/*.MOD; do
    j=`basename $i .MOD`;
    k=`dirname $i`;
    l=`basename $k`; 
    ffmpeg -ss 1 -i $i -vcodec mjpeg -vframes 1 -an -f rawvideo -s 144x100 -y /web/www-protected/Workspace/videos/$l-$j.jpg; 
    touch -r $i /web/www-protected/Workspace/videos/$l-$j.jpg; 
done

for i in /home/pictures//photos/Divers/Konica/*/*.MPG; do
    j=`basename $i .MPG`;
    k=`dirname $i`;
    l=`basename $k`; 
    ffmpeg -ss 1 -i $i -vcodec mjpeg -vframes 1 -an -f rawvideo -s 144x100 -y /web/www-protected/Workspace/videos/$l-$j.jpg; 
    touch -r $i /web/www-protected/Workspace/videos/$l-$j.jpg; 
done
