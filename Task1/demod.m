function [filtData, len] = demod(rx,startpos)
 rx = rx';
 f1=4000;
 f2=6000;
 fs=48000;
 Ts=1/fs;
 T=0.025;
 M=1200;
 totalBits=8;
 filtData=[];
 for i=1:totalBits
       fftr = fft(rx((i-1)*1200+startpos:i*1200+startpos));
       Z=abs(fftr);
       [ma, I]=max(Z);
       if (I/length(Z)*fs - 4000 >= -1000 && I/length(Z)*fs - 4000 <= 1000)
           filtData=[filtData 0];
       else
           filtData=[filtData 1];
       end
 end
 len=0;
 for i=1:8
     len=len+2^(8-i)*filtData(i);
 end
 totalBits=len;
 startpos=startpos+9600;
 filtData=[];
  for i=1:totalBits
       fftr = fft(rx((i-1)*1200+startpos:i*1200+startpos-1));
       Z=abs(fftr);
       [ma, I]=max(Z);
       if (I/length(Z)*fs - 4000 >= -500 && I/length(Z)*fs - 4000 <= 500)
           filtData=[filtData 0];
       else
           filtData=[filtData 1];
       end
  end
end

