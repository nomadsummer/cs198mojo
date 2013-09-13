library("ggplot2")

plotRuntime = TRUE
plotCommCost = TRUE
plotMaxflowCost = TRUE
plotUpdateCost = TRUE

if (plotRuntime)

{
#png("/Domain/tudelft.net/Users/rdelavizaghbolagh/Documents/triblerws/SimilDisRealExperiment/src/peersim/simildis/finalResults/plotsSampleGraph/static_dynamic.png")
runtimes <- read.table("/Domain/tudelft.net/Users/rdelavizaghbolagh/Documents/triblerws/SimilarityBasedDist/src/peersim/simildis/experimentResults/combined_runtime_erdosreny_2000.txt", skip=1, sep='|', col.names = c("num_records","num_nodes","num_edges","density","avg_runtime","se_runtime","mode"))
#p <- qplot(num_records, avg_runtime, data= runtimes,  linetype=factor(mode), geom = c("smooth","line")) + xlab("Number of received records")+ylab("runtime (ms)") 
attach(runtimes)

p1 <- qplot(num_records, avg_runtime/1000, data= runtimes,  linetype=factor(mode), geom = c("line")) + xlab("Number of received records")+ylab("cumulative update time (s)")
#limits <- aes(ymax = avg_runtime + se_runtime, ymin=avg_runtime - se_runtime)
#p1 <- p1 + geom_errorbar(limits, width=.1, position=position_dodge(.02) )
p1 <- p1+ labs(linetype="update mode")
p1 <- p1+opts(title="Dynamic and static update time comparision \n (random graph)")
p1
rm(runtimes)
dev.off()
}

if (plotCommCost)
{
dev.on()
png("/Domain/tudelft.net/Users/rdelavizaghbolagh/Documents/triblerws/SimilDisRealExperiment/src/peersim/simildis/finalResults/plotsSampleGraph/cmm_cost.png")	
commCost <- read.table("/Domain/tudelft.net/Users/rdelavizaghbolagh/Documents/triblerws/SimilDisRealExperiment/src/peersim/simildis/realTraceResults/communication_cost_2009.txt", skip=1, sep='|', col.names = c("number_of_records","network_size","total_sent_messages","avg_num_messages","std_err"))
attach(commCost)

p2 <- qplot(number_of_records, total_sent_messages, data= commCost,  geom = c("line")) + xlab("Number of processed records")+ylab("#of sent msgs")
#limits <- aes(ymax = avg_runtime + se_runtime, ymin=avg_runtime - se_runtime)
#p <- p + geom_errorbar(limits, width=.1, position=position_dodge(.02) )
#p <- p+ labs(linetype="update mode")
p2 <- p2+opts(title="Simildis Communication Cost")
p2
rm(commCost)
dev.off()
}

if (plotMaxflowCost)
{
dev.on()
png("/Domain/tudelft.net/Users/rdelavizaghbolagh/Documents/triblerws/SimilDisRealExperiment/src/peersim/simildis/finalResults/plotsSampleGraph/maxflow_cost.png")
maxflowCost <- read.table("/Domain/tudelft.net/Users/rdelavizaghbolagh/Documents/triblerws/SimilDisRealExperiment/src/peersim/simildis/realTraceResults/maxflow_cost_2009.txt", skip=1, sep='|', col.names = c("cycle","network_size","total_runtime","avg_runtime","std_err","mode"))
attach(maxflowCost)

p3 <- qplot(cycle, total_runtime/1000, data= maxflowCost,  linetype=factor(mode), geom = c("line")) + xlab("#Cycles")+ylab("# runtime (s)")
#limits <- aes(ymax = avg_runtime + se_runtime, ymin=avg_runtime - se_runtime)
#p <- p + geom_errorbar(limits, width=.1, position=position_dodge(.02) )
p3 <- p3+ labs(linetype="reputation type")
p3 <- p3+opts(title="Maxflow cost in local and global evaluations")
p3
rm(maxflowCost)
dev.off()
}

if (plotUpdateCost)
{
dev.on()	
png("/Domain/tudelft.net/Users/rdelavizaghbolagh/Documents/triblerws/SimilDisRealExperiment/src/peersim/simildis/finalResults/plotsSampleGraph/update_cost.png")	
updateCost <- read.table("/Domain/tudelft.net/Users/rdelavizaghbolagh/Documents/triblerws/SimilDisRealExperiment/src/peersim/simildis/realTraceResults/update_cost_2009.txt", skip=1, sep='|', col.names = c("cycle","network_size","total_runtime","avg_runtime","std_err"))
attach(updateCost)

p4 <- qplot(cycle, total_runtime/1000, data= updateCost, geom = c("line","smooth")) + xlab("#Cycles")+ylab("# system wide update runtime (s)")
#limits <- aes(ymax = avg_runtime + se_runtime, ymin=avg_runtime - se_runtime)
#p <- p + geom_errorbar(limits, width=.1, position=position_dodge(.02) )
#p4 <- p4+ labs(linetype="reputation type")
p4 <- p4+opts(title="Update cost")
p4
rm(updateCost)
dev.off()
}

{
reuputations <- read.table("/Domain/tudelft.net/Users/rdelavizaghbolagh/Documents/triblerws/SimilDisRealExperiment/src/peersim/simildis/realTraceResults/reputation_comp_2009.txt", skip=1, sep='|', col.names = c("index","evaluator","evaluatee","reputation"))
attach(updateCost)

p4 <- qplot(cycle, total_runtime/1000, data= updateCost, geom = c("line","smooth")) + xlab("#Cycles")+ylab("# system wide update runtime (s)")
#limits <- aes(ymax = avg_runtime + se_runtime, ymin=avg_runtime - se_runtime)
#p <- p + geom_errorbar(limits, width=.1, position=position_dodge(.02) )
#p4 <- p4+ labs(linetype="reputation type")
p4 <- p4+opts(title="Update cost")
p4

	
	}





