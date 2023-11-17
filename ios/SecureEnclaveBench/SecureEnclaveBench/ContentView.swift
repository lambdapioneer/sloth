import SwiftUI

func runExperiment(experiment: String, n: Int, iterations: Int) throws -> [Double] {
    switch(experiment){
    case "seop":
            return try SecureEnclave.runEval(iterations: iterations)
    case "sloth":
            let sloth = RainbowSloth(withN: n)
            let (storage, _) = try sloth.keygen(pw: "test", handle: "eval", outputLength: 32)
            return try sloth.eval(storageState: storage, pw: "test", outputLength: 32, iterations: iterations)
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

    // CHANGE THIS LINE TO YOUR SERVER!
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
    @State var taskSelection = "seop"
    @State var iterations = 10
    @State var n = 1
    @State var isRunning = false
    @State var taskOutput = "ready"

    var body: some View {
        VStack {
            Picker("Experiment", selection: $taskSelection, content: {
                Text("SE-OP").tag("seop")
                Text("RainbowSloth").tag("sloth")
            }).disabled(isRunning)

            TextField("N", text:Binding(
                get: { String(n) },
                set: { n = Int($0) ?? 1 }
            ))
                .keyboardType(.numberPad)
                .disabled(taskSelection != "sloth")

            Picker("How many iterations", selection: $iterations, content: {
                Text("Iterations: 1").tag(1)
                Text("Iterations: 10").tag(10)
                Text("Iterations: 100").tag(100)
                Text("Iterations: 1000").tag(1000)
            }).disabled(isRunning)

            Button("Action") {
                taskOutput = "started"
                isRunning = true
                let dispatchQueue = DispatchQueue.global(qos: .background)
                dispatchQueue.async{
                    do{
                        let output = try backgroundWork(
                            experiment: taskSelection,
                            n: n,
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
            }.padding().disabled(isRunning)
            if isRunning {
                ProgressView()
            }
            Text(taskOutput).padding()
        }
        .padding()
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
