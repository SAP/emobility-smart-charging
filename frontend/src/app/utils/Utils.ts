import { FuseTree, FuseTreeNode } from 'src/assets/server_types';


export class Utils {
    static roundToNDecimals(myNumber: number, nDecimals: number): number {
        const rounder = Math.pow(10, nDecimals); 
        return Math.round(myNumber * rounder) / rounder;  
    }

    static toHH_MM_SS(secondsAfterMidnight): string {
        var date = new Date(null);
        date.setSeconds(secondsAfterMidnight);
        var result = date.toISOString().substr(11, 8);
        return result; 
    }
    static toHH_MM(secondsAfterMidnight): string {
        var date = new Date(null);
        date.setSeconds(secondsAfterMidnight);
        var result = date.toISOString().substr(11, 5);
        return result; 
    }


    static getInfrastructureLimitW(fuseTree: FuseTree): number {
        const rootFuse = fuseTree.rootFuse; 
        return this.getFusePowerLimitW(rootFuse); 
    }
    static getFusePowerLimitW(fuseTreeNode: FuseTreeNode): number {
        return (fuseTreeNode["fusePhase1"] + fuseTreeNode["fusePhase2"] + fuseTreeNode["fusePhase3"])*230; 
    }

}