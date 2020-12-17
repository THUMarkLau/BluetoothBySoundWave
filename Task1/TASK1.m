[sig, sample_frequency]=audioread("res.wav");
csv = csvread("content.csv", 5, 3);
[row, col] = size(csv);
err = [];
for i=1:row
    startpos = csv(i, 1);
    [r, len] = demod(sig, startpos + 24000);
    result = [len r];
    [number,ratio] = biterr(r, csv(i, 2:1+len));
    err = [err ratio];
    writematrix(result, 'result.csv', 'WriteMode', 'append');
end
