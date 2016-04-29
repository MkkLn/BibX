# read command line arguments
args <- commandArgs(TRUE)

# read CSV
piex <- read.csv(paste(args[1], args[2], sep="/"), header=T, as.is=T)

# make percentage labels
pielabels <- paste(round(100*piex$count/sum(piex$count), 2), "%", sep="")

pdf(paste(args[1], args[3], sep="/"), height=8, width=8) # default dimensions=7
par(mai=c(0, 0.25, 0.5, 0.25)) # bottom, left, top, right
# pie default color vector is: c("white", "lightblue", "mistyrose", "lightcyan", "lavender", "cornsilk")
# pie(piex$count, labels=paste(piex$type, ": ", piex$count, sep=""), clockwise=TRUE, init.angle=180, density=c(0,20,40,60,80,100))
pie(piex$count, labels=pielabels, clockwise=TRUE, init.angle=180, col=gray.colors(nrow(piex), start=.98, end=0), radius=0.6)
legend("topright", piex$type, cex=1, fill=gray.colors(nrow(piex), start=.98, end=0))
dev.off()
