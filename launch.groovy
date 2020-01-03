//Uncommment this section to update device loader	
// ScriptingEngine.copyGitFile("https://github.com/OperationSmallKat/SmallKat_V2.git",
//								"https://github.com/madhephaestus/MediumKat_copy.git", 
//								"loadRobot.groovy")
MobileBase robot= ScriptingEngine.gitScriptRun(
	"https://github.com/madhephaestus/MediumKat_copy.git", 
	"loadRobot.groovy", 
	["https://github.com/madhephaestus/MediumKat_copy.git",
	"MediumKat_copy.xml",
	"GameController_22",
	"hidDevice",
	"MediumKat_copy"]
);
println robot