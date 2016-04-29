# set random seed for reproducibility
set.seed(1234)

# load igraph
if (!require("igraph")) {
	install.packages("igraph", dep=TRUE, repos="http://cran.us.r-project.org")
    if(!require("igraph")) {
		stop("Unable to install required igraph-package.")
	}
}

# read command line arguments
args <- commandArgs(TRUE)
input_file_verts <- paste(args[1], args[2], sep="/")
input_file_edges <- paste(args[1], args[3], sep="/")
output_file <- paste(args[1], args[4], sep="/")
edgesize_min <- as.integer(args[5])

# read CSVs
verts <- read.csv(input_file_verts, header=T, as.is=T)
edges <- read.csv(input_file_edges, header=T, as.is=T)

# make net
net <- graph.data.frame(edges, verts, directed=F)

# delete non-connected vertices
net <- delete.vertices(net, V(net)[degree(net)==0])

# delete edges < edgesize_min
if (edgesize_min > 1) {
	net <- delete.edges(net, E(net)[E(net)$width < edgesize_min])
	net <- delete.vertices(net, V(net)[degree(net)==0])
}

# scale the vertice and edge sizes here in R, not in Java
# old:
# if (edgesize_min == 1) {
# 	V(net)$size  <- (log(V(net)$size)*2.0)+0.3 # log(1)=0
# 	E(net)$width <- (log(E(net)$width))+0.3    # log(1)=0
# } else {
# 	V(net)$size  <- (log(V(net)$size)*2.0)
# 	E(net)$width <- (log(E(net)$width))
# }
V(net)$size  <- ((log(V(net)$size - (min(V(net)$size)-1)) * 2.0)) + 0.75
E(net)$width <- (log(E(net)$width - (min(E(net)$width)-1)) * 1.75) + 0.75

# for saving network positions:
# coords <- layout.fruchterman.reingold(net)

pdf(output_file)
par(mai=c(0.25,0.25,0.25,0.25))
plot.igraph(
	net,
	layout=layout.fruchterman.reingold,
	edge.arrow.size=0,
	edge.color=rgb(0, 0, 0, alpha=0.1+((E(net)$width/max(E(net)$width))*0.8)),
	vertex.color=rgb(1, 1-V(net)$size/max(V(net)$size), 1-V(net)$size/max(V(net)$size)),
	vertex.frame.color=rgb(1-(V(net)$size/max(V(net)$size)), 1-(V(net)$size/max(V(net)$size)), 1-(V(net)$size/max(V(net)$size))),
	vertex.label.font=2,
	vertex.label.cex=0.25+(V(net)$size/(1.75*max(V(net)$size))),
	vertex.label.dist=0.1+((V(net)$size/max(V(net)$size))*0.3),
	vertex.label.color=rgb(0, 0, 0, alpha=0.25+(V(net)$size/(1.5*max(V(net)$size))))
)
dev.off()
