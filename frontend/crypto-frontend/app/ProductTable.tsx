"use client";

import {Product} from "@/app/page";

export default function ProductTable({initialProducts}: { initialProducts: Product[] }) {
    return (
        <div>
            <form>
                <input
                    type="text"
                    placeholder="Search products..."
                    onChange={(e) => console.log(e.target.value)}
                    style={{padding: '0.5rem', marginRight: '0.5rem'}}/>
                <button type="submit">Search</button>
            </form>
            <table>
                <thead>
                <tr>
                    <th>ID</th>
                    <th>Name</th>
                    <th>Price</th>
                    <th>Category</th>
                </tr>
                </thead>
                <tbody>
                {initialProducts.length === 0 ? (
                    <tr>
                        <td colSpan="4">No products found.</td>
                    </tr>
                ) : (
                    initialProducts.map((product) => (
                        <tr key={product.id}>
                            <td>{product.id}</td>
                            <td>{product.name}</td>
                            <td>{product.price}</td>
                            <td>{product.category}</td>
                        </tr>
                    ))
                )}
                </tbody>
            </table>
        </div>
    )
}
