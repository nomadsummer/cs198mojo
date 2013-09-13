rm(list = ls())
library("ggplot2")
library("lattice")
library("Hmisc")

convergence <- read.table("/Domain/tudelft.net/Users/rdelavizaghbolagh/Documents/triblerws/SimilarityBasedDist/src/peersim/simildis/result/convergence.txt", skip=1, sep='|',
col.names = c("iteration","nodeId","similarity"))

attach(convergence)

p <- qplot(iteration, similarity, data= convergence, geom="line")  + facet_grid(nodeId ~ . ) +ylim(0,0.9)  + xlim(0,3) + geom_smooth()  + geom_point()
		
p
