import RainbowSloth
import SwiftUI

func runExperiment(experiment: String, n: Int, iterations: Int) throws -> [Double] {
    switch(experiment){
    case "seop":
        return try SecureEnclaveEvaluationWrapper.runEval(iterations: iterations)
    case "sloth":
        let sloth = RainbowSloth(withN: n)
        return try RainbowSlothEvaluationWrapper.runEval(sloth: sloth, iterations: iterations)
    default:
        assert(false)
        return [Double]()
    }
}

func backgroundWork(experiment: String, n: Int, iterations: Int) throws -> String {
    // run
    let executionTimes = try runExperiment(
        experiment: experiment,
        n: n,
        iterations: iterations
    )

    // format data as json
    let json: [String: Any] = [
        "device": UIDevice.modelName,
        "version": UIDevice.current.systemVersion,
        "experiment": experiment,
        "n": n,
        "iterations": iterations,
        "executionTimeSeconds": executionTimes
    ]
    let jsonData = try! JSONSerialization.data(withJSONObject: json)
    debugPrint("json", json)

    //
    // --> CHANGE ME <--
    //
    let url = URL(string: "http://127.0.0.1:8080/ios-report")!

    // upload
    var request = URLRequest(url: url)
    request.httpMethod = "POST"
    request.addValue("application/json", forHTTPHeaderField: "Content-Type")
    request.httpBody = jsonData

    let task = URLSession.shared.dataTask(with: request) { data, response, error in
        debugPrint(data as Any, response as Any, error as Any)
    }
    task.resume()

    // update UI
    let executionTime = executionTimes.reduce(0, +)
    let executionTimeEach = executionTime / Double(iterations) * 1000.0
    return String(format: "success\ntotal: %.3f seconds\neach: %.1f ms", executionTime, executionTimeEach)
}

struct ContentView: View {
    @State var taskSelection = "sloth"
    @State var iterations = 10
    @State var n = 10
    @State var isRunning = false
    @State var taskOutput = "ready"

    var body: some View {
        VStack(alignment: .leading) {
            Spacer()
            Text("Rainbow Sloth 🦥").font(.title)
            Spacer()
            
            Text("Experiment to run:").font(.headline)
            Picker("Experiment", selection: $taskSelection, content: {
                Text("SE-OP").tag("seop")
                Text("RainbowSloth").tag("sloth")
            }).disabled(isRunning)
            
            Text("Rainbow parameter n:").font(.headline)
            Picker("Rainbow parameter n", selection: $n, content: {
                Text("1").tag(1)
                Text("10").tag(10)
                Text("100").tag(100)
                Text("1000").tag(1000)
            }).disabled(isRunning || taskSelection != "sloth")
            
            Text("Number of experiment iterations:").font(.headline)
            Picker("Number of experiment iterations", selection: $iterations, content: {
                Text("1").tag(1)
                Text("10").tag(10)
                Text("100").tag(100)
                Text("1000").tag(1000)
            }).disabled(isRunning)
            
            Button("Start run") {
                taskOutput = "started"
                isRunning = true
                let dispatchQueue = DispatchQueue.global(qos: .background)
                dispatchQueue.async{
                    do{
                        let output = try backgroundWork(
                            experiment: taskSelection,
                            n: Int(n),
                            iterations: Int(iterations)
                        )
                        DispatchQueue.main.async {
                            taskOutput = output
                            isRunning = false
                        }
                    }catch {
                        DispatchQueue.main.async {
                            taskOutput = "error: \(error)"
                            isRunning = false
                        }
                    }
                }
            }
            .buttonStyle(.borderedProminent)
            .padding().disabled(isRunning)
            
            Text("Status/output:").font(.headline)
            if isRunning {
                ProgressView()
            }
            Text(taskOutput)
            Spacer()
        }
        .padding()
    }
}

#Preview {
    ContentView()
}
