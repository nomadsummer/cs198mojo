rm(list = ls())
library("ggplot2")
library("lattice")
library("Hmisc")

convergence <- read.table("/Domain/tudelft.net/Users/rdelavizaghbolagh/Documents/triblerws/SimilarityBasedDist/src/peersim/simildis/result/convergenceSum.txt", skip=1, sep='|', col.names = c("iteration","nodeId","similarity"))

attach(convergence)

p <- qplot(nodeId, similarity, data= convergence, geom="line", colour=factor(iteration)) +ylim(0,1.0)  +  geom_point()
#p <- qplot(iteration, similarity, data= convergence, geom="line")  + facet_grid(nodeId ~ . ) +ylim(0,0.9)  + xlim(0,1) # + geom_point()
p
