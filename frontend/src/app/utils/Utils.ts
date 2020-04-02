import { FuseTree, FuseTreeNode, Car } from 'src/assets/server_types';


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

    static getFuseTreeNodeLabel(fuseTreeNode: FuseTreeNode): string {
        let label: string;
        if (fuseTreeNode["fusePhase1"] === fuseTreeNode["fusePhase2"] &&
            fuseTreeNode["fusePhase2"] === fuseTreeNode["fusePhase3"]) {

            label = fuseTreeNode["fusePhase1"] + "A~3";
        }
        else {
            label = fuseTreeNode["fusePhase1"] + "A/" + fuseTreeNode["fusePhase2"] + "A/" + fuseTreeNode["fusePhase3"] + "A";
        }
        const powerKW = Utils.getFusePowerLimitW(fuseTreeNode) / 1000;
        label += " (" + Utils.roundToNDecimals(powerKW, 3) + "kW)";

        return label;
    }

    static getCarLabel(car: Car): string {
        const sumUsedPhases = car.canLoadPhase1 + car.canLoadPhase2 + car.canLoadPhase3; 
        return car.maxCurrentPerPhase + "A~" + sumUsedPhases + " (" + car.maxCurrent*230/1000 + "kW)"; 
    }

    // https://ourcodeworld.com/articles/read/189/how-to-create-a-file-and-generate-a-download-with-javascript-in-the-browser-without-a-server
    static createFileDownload(filename: string, text: string) {
        var element = document.createElement('a');
        element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
        element.setAttribute('download', filename);

        element.style.display = 'none';
        document.body.appendChild(element);

        element.click();

        document.body.removeChild(element);
    }


}