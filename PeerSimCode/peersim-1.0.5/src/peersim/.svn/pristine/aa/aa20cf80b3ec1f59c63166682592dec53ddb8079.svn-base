library("ggplot2")
library("lattice")
library("Hmisc")


###index|time|avg_subjective_rep|std_subjective_reputation|SE_subjective_reputation|avg_objective_rep|std_objective_rep|SE_objective_reputation|avg_difference|std_avg_difference|SE_avg_reputation

similarity_values <- read.table("/Domain/tudelft.net/Users/rdelavizaghbolagh/Documents/triblerws/pubsub/src/peersim/simildis/result/similarity_biased.txt", skip=1, sep='|',col.names = c("index","similarity"))

attach(similarity_values)
#png(myarg[5])


#png("/Domain/tudelft.net/Users/rdelavizaghbolagh/Desktop/Reputation_erorrs.png")

#limits <- aes(ymax = (avg_difference -   1.96 * SE_subjective_reputation ), ymin= (avg_difference + 1.96 * SE_subjective_reputation ) )

#p <- qplot(similarity, geom = c("point"))
#+geom_point(size=2.5, aes(shape = factor(mType)))

#p <- p+opts(title="Average reputation evaluation error in DisPerSy")

#p
#dev.off()}
hist(similarity,20)