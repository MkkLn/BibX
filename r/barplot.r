# read command line arguments
# 1: dir, 2: input filename, 3: output filename, 4: category label
args <- commandArgs(TRUE)

# read CSV
bar <- read.csv(paste(args[1], args[2], sep="/"), header=T, as.is=T)

pdf(paste(args[1], args[3], sep="/"), width=10, height=5.5) # default dimensions=7
# par(mai=c(0.25,0.25,0.25,0.25))
mp <- barplot(bar$count, names.arg=bar$categ, ylim=c(0,max(bar$count)+(50-max(bar$count) %% 50)), axes=FALSE, ylab="Count", xlab=args[4], font.lab=2)
axis(2, at=seq(0, max(bar$count)+(50-max(bar$count) %% 50), by=50))
axis(2, at=seq(0, max(bar$count)+(50-max(bar$count) %% 50), by=10), tcl=-0.1, labels=FALSE)
abline(h=seq(50, max(bar$count)-(max(bar$count) %% 50), by=50), col="gray75")
text(mp, y=bar$count, labels=bar$count, pos=3, offset=0.5, col="gray50")
dev.off()
